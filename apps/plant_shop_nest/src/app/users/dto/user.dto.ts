// # Importations
import {
  IsEmail,
  IsNotEmpty,
  IsOptional,
  IsString,
  IsBoolean,
} from 'class-validator';

// # Données (DTOs)
export class CreateUserDto {
  @IsEmail() email!: string;
  @IsString() @IsNotEmpty() name?: string;
  @IsString() @IsNotEmpty() password!: string;
  @IsBoolean() @IsOptional() admin?: boolean;
}

export class UpdateUserDto {
  @IsEmail() @IsOptional() email?: string;
  @IsString() @IsOptional() name?: string;
  @IsString() @IsOptional() password?: string;
  @IsBoolean() @IsOptional() admin?: boolean;
}
