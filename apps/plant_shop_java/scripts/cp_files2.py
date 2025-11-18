#!/usr/bin/env python3
import os
from pathlib import Path

BASE = Path("/home/tilnede0x1182/code/tilnede0x1182/Personnel/2025/Entrainement/plant_shop/DiversBackends/apps/plant_shop_java")
MONO = BASE / "Structure_monolithique" / "plant_shop_java_quarkus"
MICRO = BASE / "Structure_en_application_distribuée" / "plant_shop_java_micronaut_distribuée"
DIST = BASE / "Structure_en_application_distribuée" / "plant_shop_java_quarkus_distribuée"

MONO_FILES = {
	"BaseRepository.java",
	"ApiMapper.java",
	"DatabaseFactory.java",
	"AuthController.java",
	"SessionService.java",
	"Guards.java",
	"PlantController.java",
	"OrderController.java",
	"OrderService.java",
	"UserController.java",
	"PlantLookup.java",
}

MICRO_FILES = {
	"Request.java",
	"JsonMapper.java",
	"Pm2Manager.java",
	"SessionAuthFilter.java",
	"AuthService.java",
	"CatalogService.java",
	"OrderService.java",
	"Gateway.java",
	"GatewayHandler.java",
	"SessionContext.java",
	"RouteTarget.java",
	"User.java",
	"Plant.java",
}

IGNORE_TOP = {"db", "test", "config"}
IGNORE_FILES = {".env", ".env.example", "Makefile"}

def collect_sources(root: Path, wanted: set[str]) -> dict[str, list[Path]]:
	out: dict[str, list[Path]] = {}
	for dirpath, _, files in os.walk(root):
		rel_root = Path(dirpath).relative_to(root)
		if rel_root.parts and rel_root.parts[0] in IGNORE_TOP:
			continue
		for name in files:
			if name not in wanted or name in IGNORE_FILES:
				continue
			full_path = Path(dirpath) / name
			out.setdefault(name, []).append(full_path)
	return out

mono_sources = collect_sources(MONO, MONO_FILES)
micro_sources = collect_sources(MICRO, MICRO_FILES)

dist_index: dict[str, list[Path]] = {}
for dirpath, _, files in os.walk(DIST):
	rel_root = Path(dirpath).relative_to(DIST)
	if rel_root.parts and rel_root.parts[0] in IGNORE_TOP:
		continue
	for name in files:
		if name in IGNORE_FILES:
			continue
		full_path = Path(dirpath) / name
		dist_index.setdefault(name, []).append(full_path)

cmds: list[str] = []
for name, targets in dist_index.items():
	if name in MONO_FILES and name in mono_sources:
		sources = mono_sources[name]
	elif name in MICRO_FILES and name in micro_sources:
		sources = micro_sources[name]
	else:
		continue  # aucun mapping clair
	# on prend la première occurrence (les doublons sont rares, sinon ajuster)
	source_path = sources[0]
	for dist_path in targets:
		cmds.append(f"cp {source_path} {dist_path}")

print("# Commandes cp à exécuter :")
for cmd in cmds:
	print(cmd)
