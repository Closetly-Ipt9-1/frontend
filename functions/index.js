const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");
const vision = require("@google-cloud/vision");
const { VertexAI } = require("@google-cloud/vertexai");

admin.initializeApp();

const db = admin.firestore();
const visionClient = new vision.ImageAnnotatorClient();

exports.runVirtualTryOn =
functions.runWith({ timeoutSeconds: 540, memory: "1GB" })
.firestore
.document("tryOns/{tryOnId}")
.onCreate(async (snap, context) => {

  const data = snap.data();
  const tryOnId = context.params.tryOnId;

  const REPLICATE_API_KEY = process.env.REPLICATE_API_KEY;

  if (!REPLICATE_API_KEY) {
    console.error("Missing Replicate API key");
    return;
  }

  try {

    // Validate input
    if (!data.personImage || !data.clothImage) {
      throw new Error("Missing images");
    }

    // mark processing
    await snap.ref.update({
      status: "processing"
    });

    // start AI prediction
    const startResponse = await axios.post(
      "https://api.replicate.com/v1/predictions",
      {
        version: "REPLACE_WITH_REAL_MODEL_VERSION",
        input: {
          person_image: data.personImage,
          garment: data.clothImage
        }
      },
      {
        headers: {
          Authorization: `Token ${REPLICATE_API_KEY}`,
          "Content-Type": "application/json"
        }
      }
    );

    let prediction = startResponse.data;

    // polling loop
    while (
      prediction.status !== "succeeded" &&
      prediction.status !== "failed"
    ) {
      await new Promise(r => setTimeout(r, 3000));

      const poll = await axios.get(
        `https://api.replicate.com/v1/predictions/${prediction.id}`,
        {
          headers: {
            Authorization: `Token ${REPLICATE_API_KEY}`
          }
        }
      );

      prediction = poll.data;
    }

    if (prediction.status === "failed") {
      await snap.ref.update({ status: "failed" });
      return;
    }

    const resultImageUrl = prediction.output?.[0];

    // save result
    await snap.ref.update({
      status: "completed",
      resultImage: resultImageUrl
    });

  } catch (error) {

    console.error("Try-on error:", error);

    await snap.ref.update({
      status: "failed"
    });
  }
});



// CLOTHING SCAN (VISION AI)
exports.analyzeClothing =
functions.storage.object().onFinalize(async (object) => {

  const filePath = object.name;

  if (!filePath || !filePath.includes("clothes")) return;

  try {

    const [labelsResult] = await visionClient.labelDetection(
      `gs://${object.bucket}/${filePath}`
    );

    const labels = labelsResult.labelAnnotations.map(l => l.description);

    const [colorResult] = await visionClient.imageProperties(
      `gs://${object.bucket}/${filePath}`
    );

    const colors =
      colorResult.imagePropertiesAnnotation.dominantColors.colors;

    const dominant = colors[0]?.color;

    const docId = filePath.split("/").pop().split(".")[0];

    await db.collection("clothes").doc(docId).update({
      tags: labels,
      color: dominant
        ? `rgb(${dominant.red},${dominant.green},${dominant.blue})`
        : "unknown"
    });

  } catch (error) {
    console.error("Vision error:", error);
  }
});



// OUTFIT RECOMMENDATION (GEMINI)
const vertex_ai = new VertexAI({
  project: process.env.GCLOUD_PROJECT,
  location: "us-central1"
});

const model = vertex_ai.getGenerativeModel({
  model: "gemini-1.5-flash"
});

exports.generateOutfit =
functions.https.onRequest(async (req, res) => {

  try {

    const clothes = req.body.clothes;

    if (!clothes || clothes.length === 0) {
      return res.status(400).send("No clothes provided");
    }

    const prompt = `
    You are a fashion stylist.

    Create ONE outfit using these clothes:
    ${JSON.stringify(clothes)}

    Rules:
    - exactly 1 top
    - exactly 1 bottom
    - optionally shoes
    - match colors and style

    Return ONLY JSON:
    {
      "top": "",
      "bottom": "",
      "shoes": ""
    }
    `;

    const result = await model.generateContent(prompt);

    const text =
      result.response.candidates[0].content.parts[0].text;

    res.send(text);

  } catch (error) {

    console.error("Gemini error:", error);
    res.status(500).send("AI failed");

  }
});