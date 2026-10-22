package com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.actions;

import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CPlayerChangeFloor extends Data {
	private int depth;
	private int pos = -1;

	public CPlayerChangeFloor(int depth) {
		this.depth = depth;
		this.pos = -1;
	}
}
