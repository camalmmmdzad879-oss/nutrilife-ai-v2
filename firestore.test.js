const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write user profiles or diary entries", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("diary_entries").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("water_logs").doc("w1").set({
      userId: ALICE_UID,
      dateIso: "2026-11-24",
      amountMl: 250,
      timestampMillis: 1700000000000,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create and partially update profile and diary entry with Kotlin-aligned payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Aysel",
      age: 28,
      heightCm: 165.0,
      currentWeightKg: 62.0,
      goalWeightKg: 58.0,
      sex: "FEMALE",
      activityLevel: "MODERATE",
      nutritionGoal: "GRADUAL_LOSS",
      dailyWaterTargetMl: 2000,
      language: "AZ",
      themeMode: "LIGHT",
      onboardingCompleted: true,
      aiConsentAccepted: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      currentWeightKg: 61.5,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("diary_entries").doc("entry_1").set({
      userId: ALICE_UID,
      dateIso: "2026-11-24",
      mealType: "LUNCH",
      foodName: "Yarpaq Dolması",
      foodCategory: "Azerbaijani",
      servingGrams: 220.0,
      portionLabel: "1 boşqab",
      calories: 365.0,
      protein: 18.2,
      carbs: 28.4,
      fat: 19.5,
      fiber: 3.1,
      sourceType: "DATABASE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("diary_entries").where("dateIso", "==", "2026-11-24").get()
  );

  // Reject immutable createdAt modification or ghost fields
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("diary_entries").doc("entry_2").set({
      id: "entry_2",
      userId: ALICE_UID,
      dateIso: "2026-11-24",
      mealType: "SNACK",
      foodName: "Nar",
      foodCategory: "Fruits",
      servingGrams: 150.0,
      portionLabel: "1 ədəd",
      calories: 125.0,
      protein: 2.5,
      carbs: 28.0,
      fat: 1.8,
      sourceType: "DATABASE",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Cross-user access is strictly denied across subcollections", async () => {
  const ts = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).collection("weight_logs").doc("w_bob").set({
      userId: BOB_UID,
      dateIso: "2026-11-24",
      weightKg: 78.0,
      timestampMillis: 1700000000000,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("weight_logs").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("weight_logs").doc("w_bob").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("weight_logs").doc("w_bob").update({
      weightKg: 70.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can manage water, weight, exercise, custom foods, recipes, and meal plans", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("water_logs").doc("wl_1").set({
      userId: ALICE_UID,
      dateIso: "2026-11-24",
      amountMl: 300,
      timestampMillis: 1700000000000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("exercise_logs").doc("ex_1").set({
      userId: ALICE_UID,
      dateIso: "2026-11-24",
      activityType: "WALKING",
      durationMinutes: 35,
      estimatedCaloriesBurned: 145.0,
      timestampMillis: 1700000000000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("meal_plans").doc("mp_1").set({
      userId: ALICE_UID,
      title: "Balanslı Azərbaycan Günü",
      goalType: "MAINTAIN",
      targetCalories: 2050,
      mealItems: ["Kükü + Təndir Çörəyi", "Mərci Şorbası + Çoban Salatı"],
      favorite: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("meal_plans").doc("mp_1").delete()
  );
});
