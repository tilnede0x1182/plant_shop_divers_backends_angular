import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    SERVER_PORT = int(os.getenv("SERVER_ADDRESS", "4100"))
    DATABASE_URL = os.getenv("DATABASE_URL")
    DATABASE_USER = os.getenv("DATABASE_USER")
    DATABASE_PASS = os.getenv("DATABASE_PASS")
    JWT_SECRET = os.getenv("JWT_SECRET")

    if not DATABASE_URL:
        raise ValueError("DATABASE_URL manquante dans le fichier .env")
    if not JWT_SECRET:
        raise ValueError("JWT_SECRET manquant dans le fichier .env")

config = Config()
