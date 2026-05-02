const { onRequest } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
const { VertexAI } = require("@google-cloud/vertexai");

admin.initializeApp();
const db = admin.firestore();


// ==============================
// 🔥 INIT AI (GEMINI)
// ==============================
const vertex_ai = new VertexAI({
  project: process.env.GCLOUD_PROJECT,
  location: "us-central1",
});

const model = vertex_ai.getGenerativeModel({
  model: "gemini-1.5-flash",
});


// ==============================
// 🔥 AI OUTFIT FUNCTION
// ==============================
exports.recommendOutfit = onRequest(async (req, res) => {

  try {

    const clothes = req.body.clothes;
    const weather = req.body.weather;
    const season = req.body.season;
    const style = req.body.style;

    // ✅ check input
    if (!clothes || clothes.length === 0) {
      return res.status(400).send("No clothes provided");
    }

    // 🧠 AI prompt
    const prompt = `
You are a fashion stylist.

User style: ${style}
Weather: ${weather}
Season: ${season}

Clothes:
${JSON.stringify(clothes)}

Rules:
- choose exactly 1 top
- choose exactly 1 bottom
- optionally shoes

Return ONLY JSON:
{
  "topId": "",
  "bottomId": "",
  "shoesId": "",
  "reason": ""
}
`;

    const result = await model.generateContent(prompt);

    const text =
      result.response.candidates[0].content.parts[0].text;

    res.send(text);

  } catch (error) {
    console.error(error);
    res.status(500).send("AI failed");
  }
});