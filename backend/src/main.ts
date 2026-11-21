import fastify from 'fastify';
import cors from '@fastify/cors';
import jwt from '@fastify/jwt';
import bcrypt from 'bcryptjs';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();
const app = fastify({ logger: true });

app.register(cors, { origin: '*' });
app.register(jwt, { secret: process.env.JWT_SECRET || 'secret' });

// --- AUTH ROUTES ---
app.post('/api/v1/auth/register', async (req, reply) => {
  const { email, password } = req.body as any;
  const hashed = await bcrypt.hash(password, 10);
  const role = email === 'admin@winimeku.com' ? 'SUPER_ADMIN' : 'USER';
  try {
    const user = await prisma.user.create({ data: { email, password: hashed, role } });
    return { message: 'Register sukses', user: { email: user.email, role: user.role } };
  } catch (e) { return reply.status(400).send({ error: 'Email sudah ada' }); }
});

app.post('/api/v1/auth/login', async (req, reply) => {
  const { email, password } = req.body as any;
  const user = await prisma.user.findUnique({ where: { email } });
  if (!user || !(await bcrypt.compare(password, user.password))) {
    return reply.status(401).send({ error: 'Email/Password salah' });
  }
  const token = app.jwt.sign({ id: user.id, role: user.role });
  return { token, role: user.role };
});

// --- ANIME ROUTES ---
app.get('/api/v1/anime', async () => {
  return await prisma.anime.findMany({ include: { episodes: true } });
});

app.get('/api/v1/anime/:id', async (req) => {
  const { id } = req.params as any;
  return await prisma.anime.findUnique({ where: { id }, include: { episodes: true } });
});

app.post('/api/v1/admin/anime', async (req, reply) => {
  try { await req.jwtVerify(); } catch { return reply.status(401).send(); }
  const data = req.body as any;
  return await prisma.anime.create({ data });
});

const start = async () => {
  await app.listen({ port: Number(process.env.PORT) || 3000, host: '0.0.0.0' });
};
start();
