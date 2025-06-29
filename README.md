# 📱 Application Mobile - Malvoyant (Frontend Kotlin)

Ce dépôt contient le code source de l’application Android dédiée à l’accompagnement des personnes malvoyantes dans leur navigation en intérieur. Développée en **Kotlin** avec **Jetpack Compose** et une architecture **MVVM**, l’application intègre des fonctionnalités avancées pour l’orientation, la reconnaissance visuelle et l’accessibilité.

---

## 🧩 Fonctionnalités principales

- **🗺️ Navigation intérieure assistée**
  - Visualisation et interaction avec un plan (floorplan) de l’environnement
  - Calcul d’itinéraires sûrs en temps réel selon la position de l’utilisateur
  - Prise en compte des zones dangereuses (obstacles, zones à éviter)
  - Guidage vocal et affichage dynamique du chemin parcouru

- **📷 Reconnaissance d’image et de texte**
  - Utilisation de la caméra pour analyser l’environnement immédiat
  - Reconnaissance de texte (OCR) avec retour vocal du contenu détecté
  - Support de TensorFlow Lite et MLKit pour l’analyse d’image avancée

- **🚶 Compteur de pas & suivi de déplacement**
  - Estimation de la position via podomètre et capteurs de l’appareil
  - Affichage en temps réel de la progression sur le plan

- **🗣️ Accessibilité et interactions vocales**
  - Retours audio pour toutes les actions et résultats importants
  - Possibilité d’utiliser la reconnaissance vocale pour certaines actions
  - Gestion des permissions d’accessibilité et caméra

- **🔑 Authentification & gestion de profil**
  - Inscription, connexion, modification de profil, réinitialisation du mot de passe
  - Sécurisation des échanges avec l’API via token

---

## ⚙️ Technologies & bibliothèques utilisées

- **Kotlin**, **Jetpack Compose** (UI réactive)
- **MVVM** (architecture)
- **CameraX**, **MLKit** (reconnaissance de texte, image)
- **TensorFlow Lite** (analyse d’image avancée)
- **Socket.io** & **WebSocket** (communication temps réel, notifications)
- **Hilt** (injection de dépendance)
- **Navigation Compose** (navigation entre écrans)
- **Coil** (chargement d’images)

---

## 🏗️ Architecture du projet

- Architecture basée sur MVVM : séparation claire entre les couches modèle, vue et logique métier
- Dépendances injectées avec Hilt pour faciliter la maintenance et les tests
- Navigation centralisée pour une expérience utilisateur fluide

---

## 🚀 Installation & configuration

### Prérequis

- **Android Studio Hedgehog** ou supérieur, SDK 24+
- Accès à une API backend compatible (voir `RetrofitClient.kt`)
- Permissions : caméra, micro, accès capteurs et stockage

### Clonage du projet

```bash
git clone https://github.com/Orama4/malvoyant_front.git
cd malvoyant_front
```

### Configuration des endpoints API

Dans le fichier `api/RetrofitClient.kt`, personnalisez :

```kotlin
private const val BASE_URL = "http://<votre_ip_ou_nom_de_domaine>:<port>/api/"
```

### Lancement

1. Ouvrir le projet dans Android Studio
2. Synchroniser les dépendances Gradle
3. Lancer sur un appareil physique ou un émulateur

---

## 📂 Organisation du code (exemple)

```
malvoyant_front/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/malvoayant/
│   │   │   │   ├── NavigationLogic/
│   │   │   │   ├── data/
│   │   │   │   ├── repositories/
│   │   │   │   ├── ui/
│   │   │   │   └── viewmodel/
│   │   │   └── res/
│   └── build.gradle.kts
├── settings.gradle.kts
└── ...
```

---

## 🛡️ Sécurité & confidentialité

- Toutes les données sensibles sont sécurisées côté client
- Communications API recommandées en HTTPS en production

---

## 🤝 Contribution

Les suggestions et contributions sont encouragées ! Merci de soumettre une issue ou un pull request.

---

## 👤 Auteur & contact

- Organisation : [Orama4](https://github.com/Orama4)

---
