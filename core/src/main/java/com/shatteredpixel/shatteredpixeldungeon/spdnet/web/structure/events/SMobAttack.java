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
public class SMobAttack extends Data {
	private String name;
	private int depth;
	private int syncId;
	private int targetPos;
	private String targetName;
	private int damage;
	private String mobClass;
	private int mobPos;
}
