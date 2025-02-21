# Système de Déduplication de Fichiers

## 📋 Vue d'ensemble

Système de déduplication de fichiers avec découpage intelligent et stockage optimisé.

```
[Fichiers] → [Chunks] → [Déduplication] → [Stockage Optimisé]
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
├── [hash1]  -- Contenu du chunk 1
├── [hash2]  -- Contenu du chunk 2
└── [hash3]  -- Contenu du chunk 3
```

## 🔄 Processus

### 1. Traitement des fichiers

1. Lecture du fichier
2. Découpage en chunks de 1MB
3. Calcul du hash pour chaque chunk
4. Vérification des doublons
5. Stockage optimisé

### 2. Détection des doublons

- Utilisation du hash comme identifiant unique
- Stockage d'une seule copie physique
- Maintien des références dans la base de données

### 3. Reconstruction

1. Requête des chunks par nom de fichier
2. Récupération ordonnée depuis le stockage
3. Assemblage dans le bon ordre
4. Sauvegarde du fichier reconstruit

## 📊 Statistiques

Le système calcule automatiquement :

- Nombre de chunks uniques
- Nombre total de références
- Taux de déduplication
- Espace économisé

## 💡 Exemple d'utilisation

```java
// Initialisation
SQLDeduplicationSystem deduplicationSystem = new SQLDeduplicationSystem("SHA-256");
FileReconstructor reconstructor = new FileReconstructor(deduplicationSystem);

// Traitement
processFolder(deduplicationSystem, "data-files");

// Reconstruction
reconstructor.reconstructFile("fichier.pdf");
```

## 🔍 Exemple concret

Pour 5 copies du même fichier PDF :

- 1 seul stockage physique
- 5 entrées dans file_chunks
- ~80% d'espace économisé

## ⚙️ Configuration requise

- Java 17+
- PostgreSQL
- Espace disque pour /storage/

## ⚙️ Arguments
(Compiler le projet en JAR avant)

Pour faire un Benchmark de comparaison de compression globale et par chunk :
- java -cp out org.hetic.CompressFile compare filenameToReconstruct.txt

Pour avoir uniquement les statistiques de duplications de chunk et de gain d'espace : 
- java -cp out org.hetic.CompressFile duplicationStats filenameToReconstruct.txt
