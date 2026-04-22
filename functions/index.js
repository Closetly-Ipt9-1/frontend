const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();

const db = admin.firestore();

exports.runVirtualTryOn =
functions.firestore
.document("tryOns/{tryOnId}")
.onCreate(async (snap, context) => {

  const data = snap.data();
  const tryOnId = context.params.tryOnId;

  const REPLICATE_API_KEY =
    functions.config().replicate_api_key;

  try {

    // 1️⃣ mark processing
    await snap.ref.update({
      status: "processing"
    });

    // 2️⃣ start AI prediction
    const startResponse = await axios.post(
      "https://api.replicate.com/v1/predictions",
      {
        version: "MODEL_VERSION_ID",
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

    // 3️⃣ POLLING LOOP
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
      await snap.ref.update({
        status: "failed"
      });
      return;
    }

    const resultImageUrl = prediction.output[0];

    // 4️⃣ save result
    await snap.ref.update({
      status: "completed",
      resultImage: resultImageUrl
    });

  } catch (error) {

    console.error(error);

    await snap.ref.update({
      status: "failed"
    });
  }
});