package com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.events;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.Data;
import com.watabou.utils.Bundle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SItemDrop extends Data {
	private String name;
	private int depth;
	private int pos;
	private String item;

	public Item getItemObject() {
		if (item == null || item.isEmpty()) {
			return null;
		}
		try {
			Bundle bundle = Bundle.fromString(item);
			return (Item) bundle.get("item");
		} catch (Exception e) {
			return null;
		}
	}
}
