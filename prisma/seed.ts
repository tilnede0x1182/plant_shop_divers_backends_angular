// # Importations
import { PrismaClient, Prisma } from '@prisma/client';
import type { User, Plant } from '@prisma/client';
import { faker } from '@faker-js/faker';
import bcrypt from 'bcryptjs';
import { writeFileSync } from 'node:fs';
import { join } from 'node:path';

// # Données
const NB_ADMINS = 3;
const NB_USERS = 20;
const NB_PLANTS = 50;
const MAX_ORDERS_PER_USER = 7;
const PLANT_NAMES = [
  'Rose',
  'Tulipe',
  'Lavande',
  'Orchidée',
  'Basilic',
  'Menthe',
  'Pivoine',
  'Tournesol',
  'Cactus (Echinopsis)',
  'Bambou',
  'Camomille (Matricaria recutita)',
  'Sauge (Salvia officinalis)',
  'Romarin (Rosmarinus officinalis)',
  'Thym (Thymus vulgaris)',
  'Laurier-rose (Nerium oleander)',
  'Aloe vera',
  'Jasmin (Jasminum officinale)',
  'Hortensia (Hydrangea macrophylla)',
  'Marguerite (Leucanthemum vulgare)',
  'Géranium (Pelargonium graveolens)',
  'Fuchsia (Fuchsia magellanica)',
  'Anémone (Anemone coronaria)',
  'Azalée (Rhododendron simsii)',
  'Chrysanthème (Chrysanthemum morifolium)',
  'Digitale pourpre (Digitalis purpurea)',
  'Glaïeul (Gladiolus hortulanus)',
  'Lys (Lilium candidum)',
  'Violette (Viola odorata)',
  'Muguet (Convallaria majalis)',
  'Iris (Iris germanica)',
  'Lavandin (Lavandula intermedia)',
  'Érable du Japon (Acer palmatum)',
  'Citronnelle (Cymbopogon citratus)',
  'Pin parasol (Pinus pinea)',
  'Cyprès (Cupressus sempervirens)',
  'Olivier (Olea europaea)',
  'Papyrus (Cyperus papyrus)',
  'Figuier (Ficus carica)',
  'Eucalyptus (Eucalyptus globulus)',
  'Acacia (Acacia dealbata)',
  'Bégonia (Begonia semperflorens)',
  'Calathea (Calathea ornata)',
  'Dieffenbachia (Dieffenbachia seguine)',
  'Ficus elastica',
  'Sansevieria (Sansevieria trifasciata)',
  'Philodendron (Philodendron scandens)',
  'Yucca (Yucca elephantipes)',
  'Zamioculcas zamiifolia',
  'Monstera deliciosa',
  'Pothos (Epipremnum aureum)',
  'Agave (Agave americana)',
  'Cactus raquette (Opuntia ficus-indica)',
  'Palmier-dattier (Phoenix dactylifera)',
  'Amaryllis (Hippeastrum hybridum)',
  'Bleuet (Centaurea cyanus)',
  'Cœur-de-Marie (Lamprocapnos spectabilis)',
  'Croton (Codiaeum variegatum)',
  'Dracaena (Dracaena marginata)',
  'Hosta (Hosta plantaginea)',
  'Lierre (Hedera helix)',
  'Mimosa (Acacia dealbata)',
];

// # Classe SeedService
class SeedService {
  private prisma = new PrismaClient();

  // ## Reset
  private reset = async (): Promise<void> => {
    await this.prisma.orderItem.deleteMany();
    await this.prisma.order.deleteMany();
    await this.prisma.plant.deleteMany();
    await this.prisma.user.deleteMany();
  };

  // ## Admins
  private createAdmins = async () => {
    const admins: { email: string; password: string }[] = [];
    for (let idx = 0; idx < NB_ADMINS; idx++)
      admins.push(await this.addAdmin(idx));
    return admins;
  };

  private addAdmin = async (index: number) => {
    const email = `admin${index + 1}@planteshop.com`;
    const password = 'password';
    await this.prisma.user.create({
      data: {
        email,
        password: await bcrypt.hash(password, 10),
        admin: true,
        name: faker.person.fullName(),
      },
    });
    return { email, password };
  };

  // ## Users
  private createUsers = async () => {
    const users: { email: string; password: string }[] = [];
    for (let idx = 0; idx < NB_USERS; idx++) users.push(await this.addUser());
    return users;
  };

  private addUser = async () => {
    const password = faker.internet.password({ length: 12 });
    const email = faker.internet.email().toLowerCase();
    await this.prisma.user.create({
      data: {
        email,
        password: await bcrypt.hash(password, 10),
        admin: false,
        name: faker.person.fullName(),
      },
    });
    return { email, password };
  };

  // ## Plants
  private createPlants = async () => {
    const max = PLANT_NAMES.length;
    const plants: Plant[] = [];
    for (let idx = 0; idx < NB_PLANTS; idx++)
      plants.push(await this.addPlant(idx, max));
    return plants;
  };

  private addPlant = async (idx: number, max: number) => {
    const base = PLANT_NAMES[idx % max];
    const name =
      NB_PLANTS > max ? `${base} ${Math.floor(idx / max) + 1}` : base;
    return this.prisma.plant.create({
      data: {
        name,
        price: faker.number.int({ min: 5, max: 50 }),
        description: faker.lorem.sentence({ min: 10, max: 14 }),
        stock: faker.number.int({ min: 5, max: 30 }),
      },
    });
  };

  // ## Orders
  private createOrders = async (plants: Plant[]) => {
    for (const user of await this.prisma.user.findMany()) {
      const numberOfOrders = faker.number.int({
        min: 0,
        max: MAX_ORDERS_PER_USER,
      });
      for (let idx = 0; idx < numberOfOrders; idx++) {
        await this.createOrderForUser(user, plants);
      }
    }
  };

  private createOrderForUser = async (user: User, plants: Plant[]) => {
    let total = 0;
    const order = await this.prisma.order.create({
      data: {
        userId: user.id,
        totalPrice: 0,
        status: faker.helpers.arrayElement([
          'confirmed',
          'pending',
          'shipped',
          'delivered',
        ]),
      },
    });
    for (let iter = 0; iter < 2; iter++)
      total += await this.addItem(order.id, plants);
    await this.prisma.order.update({
      where: { id: order.id },
      data: { totalPrice: total },
    });
  };

  private addItem = async (orderId: number, plants: Plant[]) => {
    const plant = plants[Math.floor(Math.random() * plants.length)];
    if (!plant.stock) return 0;
    const qty = Math.min(faker.number.int({ min: 1, max: 5 }), plant.stock);
    if (!qty) return 0;
    await this.prisma.orderItem.create({
      data: { orderId, plantId: plant.id, quantity: qty },
    });
    await this.prisma.plant.update({
      where: { id: plant.id },
      data: { stock: plant.stock - qty },
    });
    plant.stock -= qty;
    return plant.price * qty;
  };

  // ## users.txt
  private writeUsersFile = (
    admins: { email: string; password: string }[],
    users: { email: string; password: string }[]
  ) => {
    const path = join(process.cwd(), 'users.txt');
    let txt = 'Administrateurs :\n\n';
    admins.forEach((admin) => (txt += `${admin.email} ${admin.password}\n`));
    txt += '\nUtilisateurs :\n\n';
    users.forEach((u) => (txt += `${u.email} ${u.password}\n`));
    writeFileSync(path, txt, 'utf8');
  };

  // # Main
  public run = async (): Promise<void> => {
    await this.reset();
    const admins = await this.createAdmins();
    const users = await this.createUsers();
    const plants = await this.createPlants();
    this.writeUsersFile(admins, users);
    await this.createOrders(plants);
    console.log('✅ Seed terminée. Données créées & users.txt généré.');
    await this.prisma.$disconnect();
  };
}

// # Lancement
new SeedService().run().catch((err) => {
  console.error(err);
  process.exit(1);
});
