// # Importations
import { IsInt, IsNotEmpty } from 'class-validator';

// # Données (DTOs)
export class CreateOrderItemDto {
  @IsInt() @IsNotEmpty() orderId!: number;
  @IsInt() @IsNotEmpty() plantId!: number;
  @IsInt() @IsNotEmpty() quantity!: number;
}

export class UpdateOrderItemDto {
  @IsInt() @IsNotEmpty() quantity!: number;
}
