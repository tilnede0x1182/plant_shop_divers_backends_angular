onst items = [
  { raw: '## 📦 Structure du repo' },
  { raw: '' },
  { raw: 'apps/' },

  { left: '  ├─ plant-shop-angular-universal', right: 'Frontend Angular' },
  { left: '  ├─ plant_shop_nest', right: 'Backend NestJS' },
  {
    left: '  ├─ plant_shop_manifest_ABANDON',
    right: 'Backend Manifest (abandonné)',
  },
  { left: '  ├─ plant_shop_go', right: 'Backend Golang avec GORM' },
  { left: '  ├─ plant_shop_cpp', right: 'Backend en C++' },
  { left: '  ├─ plant_shop_c', right: 'Backend en C' },

  { raw: '  ├─ plant_shop_java' },
  { left: '  │  ├── Structure_en_micro_services', right: '' },
  {
    left: '  │  │  └── plant_shop_java_microservices',
    right: 'Microservices Java',
  },
  { left: '  │  └── Structure_monolithique', right: '' },
  { left: '  │     ├── plant_shop_java', right: 'Backend en Java avec HTTP' },
  {
    left: '  │     ├── plant_shop_java_active_web',
    right: 'Backend en Java avec Java Lite (Active Web)',
  },
  {
    left: '  │     ├── plant_shop_javalin',
    right: 'Backend en Java avec Javalin',
  },
  {
    left: '  │     ├── plant_shop_java_micronaut',
    right: 'Backend en Java avec Micronaut',
  },
  {
    left: '  │     ├── plant_shop_java_quarkus',
    right: 'Backend en Java avec Quarkus',
  },

  { left: '  │     ├── plant_shop_java_spring', right: '' },
  {
    left: '  │     │  ├── plant_shop_java_spring_boot_security',
    right: 'Backend Spring Boot + Security',
  },
  {
    left: '  │     │  └── plant_shop_java_spring_boot_security_hibernate',
    right: 'Backend Spring Boot + Security + Hibernate',
  },

  {
    left: '  │     └── plant_shop_play_framework',
    right: 'Backend avec Play Framework',
  },

  { left: '  ├─ plant_shop_python', right: 'Backend en Python' },
  { left: '  ├─ plant_shop_haskell', right: 'Backend en Haskell' },

  { raw: '  └─ plant_shop_rust' },
  { left: '     ├─ plant_shop_rust_sqlx', right: 'Backend Rust avec SQLx' },
  {
    left: '     └─ plant_shop_rust_see_orm',
    right: 'Backend Rust avec SeaORM',
  },

  { raw: '' },
  { left: 'prisma/', right: 'Modèles + seed' },
  { left: 'tests/', right: 'Scripts de test des routes backend' },
  { left: 'diagnose-ora.js', right: 'Script diagnostic' },
];

// calcul du plus long "left" et rendu avec +2 espaces
const extra = 2;
let maxLeft = 0;
for (const it of items)
  if (it.left) maxLeft = Math.max(maxLeft, it.left.length);

const out = items
  .map((it) => {
    if (it.raw !== undefined) return it.raw;
    const pad = ' '.repeat(maxLeft - it.left.length + extra);
    return `${it.left}${pad}→ ${it.right}`;
  })
  .join('\n');

console.clear();
console.log(out);
