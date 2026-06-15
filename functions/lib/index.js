"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.analyzeFood = void 0;
const admin = require("firebase-admin");
const functions = require("firebase-functions");
admin.initializeApp();
const NUTRITION_PROMPT = `
Sen bir beslenme uzmanı ve yemek analiz AI'ısın.
Bu yemek görselini analiz et. Görselde birden fazla besin/yemek varsa HEPSİNİ tespit et.
TAM OLARAK aşağıdaki JSON formatında yanıt ver. Başka hiçbir metin ekleme, sadece JSON döndür:

{
  "foods": [
    {
      "name": "yemeğin adı (Türkçe)",
      "grams": <tahmini gram miktarı, tam sayı, KESİNLİKLE 100 YAZMA>,
      "calories": <bu besinin toplam kalorisi, tam sayı, SIFIR OLMAMALI>,
      "protein": <protein gram, ondalık 1 basamak>,
      "carbs": <karbonhidrat gram, ondalık 1 basamak>,
      "fat": <yağ gram, ondalık 1 basamak>,
      "fiber": <lif gram, ondalık 1 basamak>,
      "sugar": <şeker gram, ondalık 1 basamak>,
      "saturatedFat": <doymuş yağ gram, ondalık 1 basamak>,
      "sodium": <sodyum mg, tam sayı>,
      "cholesterol": <kolesterol mg, tam sayı>,
      "potassium": <potasyum mg, tam sayı>
    }
  ],
  "confidence": <0.0-1.0 arası güven skoru>
}

ÖNEMLİ KURALLAR:
1. "grams" alanını KESİNLİKLE 100 olarak varsayma! Görseldeki tabak boyutu, kaşık, çatal, bardak gibi referans noktalarına bakarak gerçekçi bir gram tahmini yap. Örneğin bir porsiyon pilav yaklaşık 150-200g, bir dilim ekmek 30-40g, bir bardak ayran 200ml, bir porsiyon makarna 200-250g.
2. "calories" alanı SIFIR olamaz — her besinin mutlaka kalori değeri vardır. Tahmini grama göre gerçek kalori değerini hesapla.
3. protein, carbs, fat, fiber, sugar, saturatedFat, sodium, cholesterol, potassium değerleri de grama orantılı GERÇEK değerler olmalı.
4. Her besin için 100g başına değil, görseldeki TAHMİNİ porsiyon miktarına göre hesapla.
5. "foods" dizisi görselde gördüğün TÜM yemek ve besinleri içersin (ekmek, pilav, salata, içecek, sos, garnitür vs. dahil).

Görselde yemek yoksa: {"error": "Yemek tespit edilemedi. Lütfen daha net bir fotoğraf deneyin."}
`;
exports.analyzeFood = functions
    .region("europe-west1")
    .runWith({ secrets: ["GEMINI_API_KEY"], timeoutSeconds: 60, memory: "256MB" })
    .https.onCall(async (data) => {
    var _a, _b, _c, _d, _e, _f, _g;
    try {
        const apiKey = process.env.GEMINI_API_KEY;
        console.log("API key mevcut:", !!apiKey);
        if (!apiKey)
            throw new functions.https.HttpsError("internal", "API anahtarı bulunamadı.");
        const { imageBase64, mimeType = "image/jpeg" } = data;
        if (!imageBase64 || imageBase64.length < 100) {
            throw new functions.https.HttpsError("invalid-argument", "Geçersiz görsel.");
        }
        console.log("Görsel uzunluğu:", imageBase64.length);
        const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=${apiKey}`;
        const body = {
            contents: [{
                    parts: [
                        { text: NUTRITION_PROMPT },
                        { inline_data: { mime_type: mimeType, data: imageBase64 } }
                    ]
                }],
            generationConfig: { temperature: 0.3, maxOutputTokens: 1024 }
        };
        console.log("Gemini API çağrılıyor...");
        const response = await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        });
        const json = await response.json();
        console.log("HTTP status:", response.status);
        if (!response.ok) {
            const errMsg = ((_a = json === null || json === void 0 ? void 0 : json.error) === null || _a === void 0 ? void 0 : _a.message) || `HTTP ${response.status}`;
            console.error("Gemini API hatası:", errMsg);
            throw new functions.https.HttpsError("internal", "Gemini hatası: " + errMsg);
        }
        const rawText = (_g = (_f = (_e = (_d = (_c = (_b = json === null || json === void 0 ? void 0 : json.candidates) === null || _b === void 0 ? void 0 : _b[0]) === null || _c === void 0 ? void 0 : _c.content) === null || _d === void 0 ? void 0 : _d.parts) === null || _e === void 0 ? void 0 : _e[0]) === null || _f === void 0 ? void 0 : _f.text) !== null && _g !== void 0 ? _g : "";
        console.log("Gemini yanıtı:", rawText.substring(0, 300));
        const jsonMatch = rawText.match(/\{[\s\S]*\}/);
        if (!jsonMatch)
            throw new functions.https.HttpsError("internal", "AI yanıtı JSON içermiyor.");
        const parsed = JSON.parse(jsonMatch[0]);
        if ("error" in parsed)
            return { error: parsed.error };
        const foods = [];
        if (Array.isArray(parsed.foods)) {
            for (const f of parsed.foods) {
                foods.push({
                    name: String(f.name || "Bilinmeyen"),
                    grams: Math.max(0, Math.round(Number(f.grams) || 100)),
                    calories: Math.max(0, Math.round(Number(f.calories) || 0)),
                    protein: Math.max(0, parseFloat((Number(f.protein) || 0).toFixed(1))),
                    carbs: Math.max(0, parseFloat((Number(f.carbs) || 0).toFixed(1))),
                    fat: Math.max(0, parseFloat((Number(f.fat) || 0).toFixed(1))),
                    fiber: Math.max(0, parseFloat((Number(f.fiber) || 0).toFixed(1))),
                    sugar: Math.max(0, parseFloat((Number(f.sugar) || 0).toFixed(1))),
                    saturatedFat: Math.max(0, parseFloat((Number(f.saturatedFat) || 0).toFixed(1))),
                    sodium: Math.max(0, Math.round(Number(f.sodium) || 0)),
                    cholesterol: Math.max(0, Math.round(Number(f.cholesterol) || 0)),
                    potassium: Math.max(0, Math.round(Number(f.potassium) || 0)),
                });
            }
        }
        if (foods.length === 0) {
            foods.push({
                name: "Bilinmeyen Yemek", grams: 100,
                calories: 0, protein: 0, carbs: 0, fat: 0,
                fiber: 0, sugar: 0, saturatedFat: 0,
                sodium: 0, cholesterol: 0, potassium: 0,
            });
        }
        const totals = foods.reduce((acc, f) => ({
            calories: acc.calories + f.calories,
            protein: acc.protein + f.protein,
            carbs: acc.carbs + f.carbs,
            fat: acc.fat + f.fat,
            fiber: acc.fiber + f.fiber,
            sugar: acc.sugar + f.sugar,
            saturatedFat: acc.saturatedFat + f.saturatedFat,
            sodium: acc.sodium + f.sodium,
            cholesterol: acc.cholesterol + f.cholesterol,
            potassium: acc.potassium + f.potassium,
        }), {
            calories: 0, protein: 0, carbs: 0, fat: 0,
            fiber: 0, sugar: 0, saturatedFat: 0,
            sodium: 0, cholesterol: 0, potassium: 0,
        });
        return {
            foods,
            foodName: foods.map(f => f.name).join(", "),
            servingSize: `${((_a = foods[0]) === null || _a === void 0 ? void 0 : _a.grams) || 100}g`,
            calories: totals.calories,
            protein: parseFloat(totals.protein.toFixed(1)),
            carbs: parseFloat(totals.carbs.toFixed(1)),
            fat: parseFloat(totals.fat.toFixed(1)),
            fiber: parseFloat(totals.fiber.toFixed(1)),
            sugar: parseFloat(totals.sugar.toFixed(1)),
            saturatedFat: parseFloat(totals.saturatedFat.toFixed(1)),
            sodium: totals.sodium,
            cholesterol: totals.cholesterol,
            potassium: totals.potassium,
            confidence: Math.min(1, Math.max(0, Number(parsed.confidence) || 0.5)),
        };
    }
    catch (error) {
        if (error instanceof functions.https.HttpsError)
            throw error;
        console.error("Beklenmeyen hata:", error.message);
        throw new functions.https.HttpsError("internal", "Hata: " + error.message);
    }
});
//# sourceMappingURL=index.js.map
