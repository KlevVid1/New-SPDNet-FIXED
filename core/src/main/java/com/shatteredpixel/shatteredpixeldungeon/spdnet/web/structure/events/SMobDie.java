package com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.events;

import com.shatteredpixel.shatteredpixeldungeon.spdnet.web.structure.Data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SMobDie extends Data {
	private String name;
	private int depth;
	private int syncId;
	private int pos;
	private String mobClass;

	public SMobDie(String name, int depth, int syncId, int pos) {
		this(name, depth, syncId, pos, null);
	}
}
