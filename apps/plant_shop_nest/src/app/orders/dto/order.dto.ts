// # Importations
import {
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsString,
  IsArray,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';

// # Données (DTOs)
export class OrderItemDto {
  @IsInt() @IsNotEmpty() plantId!: number;
  @IsInt() @IsNotEmpty() quantity!: number;
}

export class CreateOrderDto {
  @IsInt() @IsNotEmpty() userId!: number;
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => OrderItemDto)
  items!: OrderItemDto[];
}

export class UpdateOrderDto {
  @IsString() @IsOptional() status?: string;
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => OrderItemDto)
  @IsOptional()
  items?: OrderItemDto[];
}
