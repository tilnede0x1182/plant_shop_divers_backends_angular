const items = [
  { raw: '## 📦 Structure du repo' },
  { raw: '' },
  { raw: 'apps/' },

  { left: '  ├─ plant-shop-angular-universal', right: 'Frontend Angular + SSR' },
  { left: '  ├─ plant_shop_nest', right: 'Backend NestJS' },
  { left: '  ├─ plant_shop_manifest', right: 'Backend Manifest' },
  { left: '  ├─ plant_shop_go', right: 'Backend Golang avec GORM' },
  { left: '  ├─ plant_shop_cpp', right: 'Backend en C++ (Drogon)' },
  { left: '  ├─ plant_shop_c', right: 'Backend en C (HTTPServer)' },
  {
    left: '  ├─ plant_shop_c-sharp',
    right: 'Backends .NET (Dapper, EF Core, MVC C#)',
  },
  {
    left: '  ├─ plant_shop_node_js',
    right: 'Backends Node.js/TypeScript MVC',
  },

  { raw: '  ├─ plant_shop_java' },
  { left: '  │  ├── Structure_en_micro_services', right: '' },
  {
    left: '  │  │  ├── plant_shop_java_microservices',
    right: 'Microservices Java (HTTP)',
  },
  {
    left: '  │  │  ├── plant_shop_java_javalin_microservices',
    right: 'Microservices Java avec Javalin',
  },
  {
    left: '  │  │  ├── plant_shop_java_micronaut_microservices',
    right: 'Microservices Java avec Micronaut',
  },
  {
    left: '  │  │  ├── plant_shop_java_quarkus_microservices',
    right: 'Microservices Java avec Quarkus',
  },
  {
    left: '  │  │  └── plant_shop_java_spring_microservices',
    right: 'Microservices Java avec Spring Boot',
  },
  { left: '  │  ├── Structure_en_application_distribuée', right: '' },
  {
    left: '  │  │  ├── plant_shop_java_distribuée',
    right: 'Architecture distribuée (HTTP)',
  },
  {
    left: '  │  │  ├── plant_shop_java_javalin_distribuée',
    right: 'Architecture distribuée avec Javalin',
  },
  {
    left: '  │  │  ├── plant_shop_java_micronaut_distribuée',
    right: 'Architecture distribuée avec Micronaut',
  },
  {
    left: '  │  │  ├── plant_shop_java_quarkus_distribuée',
    right: 'Architecture distribuée avec Quarkus',
  },
  {
    left: '  │  │  └── plant_shop_java_spring_distribuée',
    right: 'Architecture distribuée avec Spring Boot',
  },
  { left: '  │  └── Structure_monolithique', right: '' },
  { left: '  │     ├── plant_shop_java', right: 'Backend en Java avec HTTP' },
  {
    left: '  │     ├── plant_shop_java_active_web',
    right: 'Backend Java ActiveWeb',
  },
  {
    left: '  │     ├── plant_shop_javalin',
    right: 'Backend Java Javalin',
  },
  {
    left: '  │     ├── plant_shop_java_micronaut',
    right: 'Backend Java Micronaut',
  },
  {
    left: '  │     ├── plant_shop_java_quarkus',
    right: 'Backend Java Quarkus',
  },
  { left: '  │     └── plant_shop_java_spring', right: 'Backend Java Spring Boot' },

  { left: '  ├─ plant_shop_python', right: 'Backend en Python (Flask)' },
  { left: '  ├─ plant_shop_haskell', right: 'Backend en Haskell (Stack)' },

  { raw: '  └─ plant_shop_rust' },
  { left: '     ├─ plant_shop_rust_sqlx', right: 'Backend Rust avec SQLx' },
  {
    left: '     └─ plant_shop_rust_see_orm',
    right: 'Backend Rust avec SeaORM',
  },

  { raw: '' },
  { left: 'prisma/', right: 'Modèles Prisma + seed' },
  { left: 'tests/', right: 'Scripts de tests E2E/Routes' },
  { left: 'diagnose-ora.js', right: 'Script diagnostic Oracle' },
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
