# ChartAnalysis/ -- TIDAK ADA FILE DI SINI

Sama seperti RiskCalculator/ -- Prioritas 8 (tombol "Analisa Chart":
Screenshot Capture, Bitmap, Crop, Compression, Multipart Upload, Gateway
Request, Bearer Token, JSON Response, Error Dialog) butuh file:

1. Handler tombol "Analisa Chart".
2. Fungsi screenshot/capture (di desktop biasanya lewat
   `java.awt.Robot` + `BufferedImage`, BUKAN Android `Bitmap` -- catat ini
   kalau kode aslinya ternyata masih pakai istilah/API Android, berarti ada
   kebingungan platform yang perlu diklarifikasi duluan).
3. Kode upload (multipart) ke AI Gateway.

Kirimkan file-file itu kalau mau saya telusuri end-to-end seperti yang
diminta di Prioritas 8.
