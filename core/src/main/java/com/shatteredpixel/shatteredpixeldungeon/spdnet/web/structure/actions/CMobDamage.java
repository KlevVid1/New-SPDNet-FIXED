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
public class CMobDamage extends Data {
	private int depth;
	private int syncId;
	private int pos;
	private int damage;
	private int currentHP;
	private String attackerName;
	private String mobClass;

	public CMobDamage(int depth, int syncId, int pos, int damage, int currentHP, String attackerName) {
		this(depth, syncId, pos, damage, currentHP, attackerName, null);
	}
}
