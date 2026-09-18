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
public class CMobSpawn extends Data {
	private int depth;
	private int syncId;
	private String mobClass;
	private int pos;
	private int hp;
	private int ht;
}
