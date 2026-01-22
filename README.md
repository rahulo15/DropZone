# 📂 DropZone

**DropZone** is a secure, self-hosted file sharing service for Local Area Networks (LAN). It enables fast, ephemeral file transfers between devices (Laptop ↔ Mobile) with military-grade encryption and privacy controls.

## ✨ Key Features

* **AES-256 Encryption:** Files are encrypted at rest (Video/Audio skipped for streaming performance).
* **Ephemeral Storage:** Auto-deletion based on time (e.g., 10 mins) or download count (e.g., 1-time view).
* **Secure Access:** Optional password protection with a "Verify-then-Download" gate.
* **Mobile Ready:** Generates dynamic QR codes for instant transfer to phones.
* **High Performance:** Uses 64KB buffered I/O streams for efficient handling of large files.

## 🛠️ Tech Stack

* **Java 17** + **Spring Boot 3**
* **SQLite** (Embedded Database)
* **Vanilla JS** + **HTML5** (No complex frontend build)
* **Maven**

## 🚀 Quick Start

### 1. Build & Run
```bash
git clone [https://github.com/yourusername/dropzone.git](https://github.com/yourusername/dropzone.git)
cd dropzone
mvn spring-boot:run
