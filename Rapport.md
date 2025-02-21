# Système de Déduplication et Compression de Fichiers

### Autheur: Julien HUYNH, Samuel LUNION

## 📋 Vue d'ensemble

Système avancé de déduplication de fichiers avec découpage intelligent, compression et stockage optimisé.

```
[Fichiers] → [Chunking Adaptatif] → [Compression] → [Déduplication] → [Stockage Optimisé]
```

## 🏗️ Architecture

### Base de données

#### Table `chunks`

```sql
CREATE TABLE chunks (
    chunk_hash VARCHAR(64) PRIMARY KEY,  -- Hash unique du chunk
    file_path TEXT NOT NULL,             -- Chemin de stockage
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### Table `file_chunks`

```sql
CREATE TABLE file_chunks (
    id SERIAL PRIMARY KEY,
    filename TEXT NOT NULL,              -- Nom du fichier original
    chunk_hash VARCHAR(64),              -- Référence au chunk
    chunk_number INT NOT NULL,           -- Position dans le fichier
    UNIQUE(filename, chunk_number)
);
```

### Structure de stockage

```
/storage/
├── [hash1]  -- Contenu compressé du chunk 1 avec métadonnées
├── [hash2]  -- Contenu compressé du chunk 2 avec métadonnées
└── [hash3]  -- Contenu compressé du chunk 3 avec métadonnées
```

## 🔄 Processus

### 1. Découpage Adaptatif (Content-Defined Chunking)

Le système utilise l'algorithme de Rabin pour un découpage intelligent adapté à la taille du fichier :

- < 10 KB: chunks de ~1 KB (512-2048 bytes)
- < 100 KB: chunks de ~4 KB (1024-4096 bytes)
- < 1 MB: chunks de ~8 KB (2048-8192 bytes)
- < 10 MB: chunks de ~16 KB (4096-16384 bytes)
- < 100 MB: chunks de ~32 KB (8192-32768 bytes)
- > 100 MB: chunks de ~64 KB (16384-65536 bytes)

### 2. Compression

Utilise la bibliothèque Zstd avec :
- Niveau de compression : 22 (maximum)
- Format des chunks compressés : [taille originale (8 bytes)][données compressées]
- Détection automatique des fichiers pré-compressés (jpg, png, zip, etc.)

### 3. Déduplication et Stockage

1. Calcul du hash SHA-256 pour chaque chunk
2. Vérification des doublons dans la base de données
3. Stockage unique des chunks compressés
4. Maintien des références et métadonnées

### 4. Reconstruction

1. Requête des chunks par nom de fichier
2. Décompression des chunks
3. Assemblage dans l'ordre original
4. Sauvegarde du fichier reconstruit

## 📊 Statistiques et Monitoring

Le système fournit des analyses détaillées :

### Déduplication
- Nombre de chunks uniques
- Nombre total de références
- Taux de déduplication
- Espace économisé

### Compression
- Taille originale vs compressée par chunk
- Ratio de compression moyen
- Performance par type de fichier
- Temps d'exécution

## 🔬 Benchmarking

Le système inclut des outils de benchmark pour comparer :
- Compression globale vs par chunk
- Temps d'exécution
- Ratios de compression
- Recommandations adaptées au type de fichier

## 💡 Exemple d'utilisation

```java
// Initialisation avec compression
CompressedChunkStorageSystem storageSystem = new CompressedChunkStorageSystem();
FileReconstructor reconstructor = new FileReconstructor(storageSystem);

// Traitement d'un dossier
processFolder(storageSystem, "data-files");

// Affichage des statistiques
storageSystem.printChunkDetails();
storageSystem.printCompressionStats();

// Reconstruction
reconstructor.reconstructFile("fichier.txt");
```

## 🔍 Exemple de performance

Pour un ensemble de fichiers texte :
- Déduplication : ~60-80% d'espace économisé
- Compression : ~40-60% de réduction supplémentaire
- Temps de traitement : quelques ms par MB
- Reconstruction instantanée

## ⚙️ Configuration requise

- Java 17+
- PostgreSQL 12+
- Bibliothèques :
  - zstd-jni (compression)
  - rabin-fingerprint (chunking)
  - HikariCP (pool de connexions)
- Espace disque pour /storage/

## 🛠️ Maintenance

- Les chunks sont datés (created_at)
- Le stockage et la base peuvent être réinitialisés
- Monitoring des ratios de compression
- Détection automatique des fichiers problématiques