package com.harry.wildcraft.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

public class BrookTroutItem extends Item {
    public static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationMod(0.1f)
            .build();

    public BrookTroutItem(Properties props) {
        super(props.food(FOOD));
    }
}
