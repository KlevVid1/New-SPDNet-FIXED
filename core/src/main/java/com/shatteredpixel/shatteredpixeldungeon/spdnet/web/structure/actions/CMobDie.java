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
public class CMobDie extends Data {
	private int depth;
	private int syncId;
	private int pos;
	private String mobClass;

	public CMobDie(int depth, int syncId, int pos) {
		this(depth, syncId, pos, null);
	}
}
