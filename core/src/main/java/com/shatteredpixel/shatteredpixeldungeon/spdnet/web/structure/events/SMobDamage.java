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
public class SMobDamage extends Data {
	private String name;
	private int depth;
	private int syncId;
	private int pos;
	private int damage;
	private int currentHP;
	private String attackerName;
	private String mobClass;

	public SMobDamage(String name, int depth, int syncId, int pos, int damage, int currentHP, String attackerName) {
		this(name, depth, syncId, pos, damage, currentHP, attackerName, null);
	}
}
