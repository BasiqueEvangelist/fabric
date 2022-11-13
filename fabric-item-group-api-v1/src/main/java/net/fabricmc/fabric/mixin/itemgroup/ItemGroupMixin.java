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

import java.util.LinkedList;
import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStackSet;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.IdentifiableItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.impl.itemgroup.FabricItemGroup;
import net.fabricmc.fabric.impl.itemgroup.ItemGroupEventsImpl;
import net.fabricmc.fabric.impl.itemgroup.MinecraftItemGroups;

@Mixin(ItemGroup.class)
abstract class ItemGroupMixin implements IdentifiableItemGroup, FabricItemGroup {
	@Shadow(aliases = "field_40859")
	private ItemStackSet displayStacks;

	@Shadow(aliases = "field_40860")
	private ItemStackSet searchTabStacks;

	@Unique
	private int fabric_page = -1;

	@Unique
	@Nullable
	private UUID fabric_fallbackUUID;

	@SuppressWarnings("ConstantConditions")
	@Inject(method = "updateEntries", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemGroup;reloadSearchProvider()V"))
	public void getStacks(FeatureSet enabledFeatures, boolean operatorEnabled, CallbackInfo ci) {
		ItemGroup self = (ItemGroup) (Object) this;

		// Do not modify special item groups (except Operator Blocks) at all.
		// Special item groups include Saved Hotbars, Search, and Survival Inventory.
		// Note, search gets modified as part of the parent item group.
		if (self.isSpecial() && self != ItemGroups.OPERATOR) return;

		// Sanity check for the injection point. It should be after these fields are set.
		Objects.requireNonNull(displayStacks, "displayStacks");
		Objects.requireNonNull(searchTabStacks, "searchTabStacks");

		// Convert the entries to lists
		var mutableDisplayStacks = new LinkedList<>(displayStacks);
		var mutableSearchTabStacks = new LinkedList<>(searchTabStacks);
		var entries = new FabricItemGroupEntries(enabledFeatures, mutableDisplayStacks, mutableSearchTabStacks, operatorEnabled);

		final Event<ItemGroupEvents.ModifyEntries> modifyEntriesEvent = ItemGroupEventsImpl.getModifyEntriesEvent(getId());

		if (modifyEntriesEvent != null) {
			modifyEntriesEvent.invoker().modifyEntries(entries);
		}

		// Now trigger the global event
		if (self != ItemGroups.OPERATOR || ItemGroups.operatorEnabled) {
			ItemGroupEvents.MODIFY_ENTRIES_ALL.invoker().modifyEntries(self, entries);
		}

		// Convert the stacks back to sets after the events had a chance to modify them
		displayStacks.clear();
		displayStacks.addAll(mutableDisplayStacks);

		searchTabStacks.clear();
		searchTabStacks.addAll(mutableSearchTabStacks);
	}

	@Override
	public Identifier getId() {
		final Identifier identifier = MinecraftItemGroups.GROUP_ID_MAP.get((ItemGroup) (Object) this);

		if (identifier == null) {
			if (fabric_fallbackUUID == null) {
				fabric_fallbackUUID = UUID.randomUUID();
			}

			// Fallback when no ID is found for this ItemGroup.
			return new Identifier("minecraft", "unidentified_" + fabric_fallbackUUID);
		}

		return identifier;
	}

	@Override
	public int getPage() {
		if (fabric_page < 0) {
			throw new IllegalStateException("Item group has no page");
		}

		return fabric_page;
	}

	@Override
	public void setPage(int page) {
		this.fabric_page = page;
	}
}