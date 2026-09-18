package com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions;

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
public class CItemDrop extends Data {
	private int depth;
	private int pos;
	private String item;

	public CItemDrop(int depth, int pos, Item itemObj) {
		this.depth = depth;
		this.pos = pos;
		Bundle bundle = new Bundle();
		bundle.put("item", itemObj);
		this.item = bundle.toString();
	}
}
