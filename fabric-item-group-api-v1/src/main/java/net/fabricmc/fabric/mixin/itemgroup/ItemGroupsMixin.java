/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.mixin.itemgroup;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;

@Mixin(ItemGroups.class)
abstract class ItemGroupsMixin {
	@Inject(method = "asArray", at = @At("HEAD"))
	private static void asArray(ItemGroup[] groups, CallbackInfoReturnable<ItemGroup[]> cir) {
		// Ensure that all item groups have a nonnull id.
		for (ItemGroup group : groups) {
			if (group.getId() == null) {
				throw new NullPointerException("Item group %s has a null id".formatted(group.getClass().getName()));
			}
		}
	}
}
