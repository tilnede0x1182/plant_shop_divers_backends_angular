// # Importations
import {
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsPositive,
  IsString,
} from 'class-validator';

// # Données (DTOs)
export class CreatePlantDto {
  /** Nom de la plante */
  @IsString() @IsNotEmpty() name!: string;
  /** Prix unitaire */
  @IsInt() @IsPositive() price!: number;
  /** Description optionnelle */
  @IsString() @IsOptional() description?: string;
  /** Stock disponible */
  @IsInt() @IsPositive() stock!: number;
}

export class UpdatePlantDto {
  @IsString() @IsOptional() name?: string;
  @IsInt() @IsPositive() @IsOptional() price?: number;
  @IsString() @IsOptional() description?: string;
  @IsInt() @IsPositive() @IsOptional() stock?: number;
}
