import type { Env } from "./types";
import { GeminiError } from "./gemini";

/**
 * OCR (Fase 6, Konstitusi bagian BACKEND).
 *
 * CATATAN JUJUR soal implementasi ini -- baca sebelum pakai di produksi:
 *  - Ini BUKAN OCR pixel-level tradisional (semacam Tesseract/Cloud Vision
 *    text-detection). Cloudflare Workers AI belum punya model text-detection
 *    presisi-tinggi yang setara itu di katalognya saat ini.
 *  - Implementasi ini memakai Gemini vision YANG SAMA dengan `/api/v1/analyze`,
 *    tapi dengan prompt yang SENGAJA dipersempit: HANYA diminta membaca ulang
 *    angka/label yang terlihat di gambar (harga, level SNR, dsb), TIDAK
 *    diminta membuat kesimpulan trading (trend/signal/entry) seperti endpoint
 *    /analyze. Tujuannya supaya bacaan angka lebih presisi/tidak "terselip"
 *    di antara reasoning naratif model.
 *  - Ini TETAP bisa halusinasi seperti model vision-language lainnya --
 *    kalau butuh jaminan akurasi karakter-per-karakter (mis. untuk keputusan
 *    finansial otomatis tanpa review manusia), pertimbangkan ganti ke
 *    Cloudflare Workers AI model OCR/text-detection begitu tersedia, atau
 *    Google Cloud Vision API murni (text_detection), bukan Gemini generatif.
 *  - Endpoint ini BELUM PERNAH di-deploy/dites sungguhan (sandbox saya tidak
 *    ada akses ke Cloudflare/Gemini API untuk verifikasi end-to-end).
 */

interface RawOcrLabel {
  text?: string;
  confidence?: number;
}

export async function extractLabelsWithGemini(
  env: Env,
  imageBase64: string,
  mimeType: string
): Promise<Array<{ text: string; confidence: number }>> {
  const prompt = buildOcrPrompt();
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${env.GEMINI_MODEL}:generateContent`;

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-goog-api-key": env.GEMINI_API_KEY
    },
    body: JSON.stringify({
      contents: [
        {
          parts: [
            { text: prompt },
            { inline_data: { mime_type: mimeType, data: imageBase64 } }
          ]
        }
      ],
      // Suhu rendah -- ini tugas pembacaan ulang, bukan tugas kreatif/analisa.
      generationConfig: { temperature: 0.0 }
    })
  });

  if (!response.ok) {
    const errText = await response.text().catch(() => "");
    throw new GeminiError(`Gemini API error ${response.status}: ${errText.slice(0, 300)}`, response.status);
  }

  const data = (await response.json()) as {
    candidates?: Array<{ content?: { parts?: Array<{ text?: string }> } }>;
  };

  const rawText = data.candidates?.[0]?.content?.parts?.find((p) => p.text)?.text;
  if (!rawText) {
    throw new GeminiError("Gemini tidak mengembalikan teks OCR", 502);
  }

  return parseOcrJson(rawText);
}

function buildOcrPrompt(): string {
  return [
    "Kamu bertugas MEMBACA ULANG (bukan menganalisa) semua teks & angka yang",
    "terlihat jelas di gambar chart trading ini: harga, level support/resistance,",
    "label SL/TP, timestamp, nama pair, dsb.",
    "",
    "ATURAN KETAT:",
    "- JANGAN membuat kesimpulan trading, JANGAN menambahkan interpretasi.",
    "- Kalau tidak yakin/tulisan terpotong/blur, tetap sertakan tapi beri confidence rendah.",
    "- Balas HANYA JSON array valid, tanpa markdown, tanpa teks lain, format persis:",
    '[{"text": "1.10523", "confidence": 0.95}, {"text": "XAUUSD", "confidence": 0.99}]'
  ].join("\n");
}

function parseOcrJson(rawText: string): Array<{ text: string; confidence: number }> {
  const cleaned = rawText.trim().replace(/^```json/, "").replace(/^```/, "").replace(/```$/, "").trim();

  let raw: RawOcrLabel[];
  try {
    const parsed = JSON.parse(cleaned);
    if (!Array.isArray(parsed)) throw new Error("bukan array");
    raw = parsed;
  } catch {
    throw new GeminiError("Gagal parse JSON OCR dari Gemini: " + cleaned.slice(0, 200), 502);
  }

  return raw
    .filter((item) => typeof item.text === "string" && item.text.trim().length > 0)
    .map((item) => ({
      text: item.text!.trim(),
      confidence: typeof item.confidence === "number" ? Math.max(0, Math.min(1, item.confidence)) : 0.5
    }));
}
