#!/usr/bin/env python3
import os
from pathlib import Path

BASE = Path("/home/tilnede0x1182/code/tilnede0x1182/Personnel/2025/Entrainement/plant_shop/DiversBackends/apps/plant_shop_java")
MONO = BASE / "Structure_monolithique" / "plant_shop_java_quarkus"
DIST = BASE / "Structure_en_application_distribuée" / "plant_shop_java_quarkus_distribuée"

# Collecte des fichiers sources dans le monolithe
monolith_files = []
for root, _, files in os.walk(MONO):
	for name in files:
		full_path = Path(root) / name
		# On ignore Makefile, db, test, seed, .env (vous les gérez)
		rel = full_path.relative_to(MONO)
		if rel.parts[0] in {"db", "test", "config"}:
			continue
		if name in {".env", ".env.example", "Makefile"}:
			continue
		monolith_files.append((name, full_path))

# Indexation des fichiers dans le distribué par nom
dist_index = {}
for root, _, files in os.walk(DIST):
	for name in files:
		full_path = Path(root) / name
		rel = full_path.relative_to(DIST)
		if rel.parts[0] in {"db", "test", "config"}:
			continue
		dist_index.setdefault(name, []).append(full_path)

# Génération des commandes cp
cmds = []
for name, mono_path in monolith_files:
	targets = dist_index.get(name, [])
	for dist_path in targets:
		# On saute les fichiers identiques (si vous voulez éviter d’écraser les .env, etc.)
		if name in {".env", ".env.example"}:
			continue
		cmd = f"cp {mono_path} {dist_path}"
		cmds.append(cmd)

# Affichage
print("# Commandes cp à exécuter :")
for cmd in cmds:
	print(cmd)
