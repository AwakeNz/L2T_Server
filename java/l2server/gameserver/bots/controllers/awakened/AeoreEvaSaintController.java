/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package l2server.gameserver.bots.controllers.awakened;

import java.util.ArrayList;

import l2server.gameserver.bots.controllers.MageController;
import l2server.gameserver.model.actor.L2Character;
import l2server.gameserver.model.actor.L2Summon;
import l2server.gameserver.model.actor.instance.L2PcInstance;
import l2server.util.Rnd;

/**
 * 
 * @author LittleHakor
 */
public class AeoreEvaSaintController extends MageController
{
	private static final int MENTAL_SHIELD_ID = 1035;
	private static final int RESIST_SHOCK_ID = 1259;
	private static final int CLARITY_ID = 1397;
	
	private final static int[] ESSENTIAL_BUFF_SKILL_IDS =
	{
		MENTAL_SHIELD_ID,
		RESIST_SHOCK_ID,
		CLARITY_ID
	};
	
	private static final int BATTLE_HEAL_ID = 1015;
	
	private static final int GREATER_HEAL_ID = 1217;
	private static final int GREATER_GROUP_HEAL_ID = 1219;

	private static final int RECHARGE_ID = 1013;
	
	private static final int MANA_BURN_ID = 1398;
	private static final int MAJOR_HEAL_ID = 1401;

	@SuppressWarnings("unused")
	private static final int BLESSING_OF_EVA_ID = 1506;
	@SuppressWarnings("unused")
	private static final int MASS_CURE_POISON_ID = 1550;
	@SuppressWarnings("unused")
	private static final int MASS_VITALIZE_ID = 1552;

	private static final int CHAIN_HEAL_ID = 1553;

	private static final int TRANCE_ID = 1394;
	private static final int ERASE_ID = 1395;
	@SuppressWarnings("unused")
	private static final int BLOCK_WINDWALK_ID = 1359;

	@SuppressWarnings("unused")
	private static final int ENLIGHTENMENT = 1533;
	
	public AeoreEvaSaintController(final L2PcInstance player)
	{
		super(player);
	}
	
	@Override
	protected int pickSpecialSkill(final L2Character target)
	{
		L2Character targetToHeal = null;
		
		int amountOfTargetsNeedingHelp = 0;
		
		// If we're under 75% HP, we prioritize healing ourself over anything else...
		if (getHealthPercent(_player) < 75)
		{
			targetToHeal = _player;
			
			amountOfTargetsNeedingHelp++;
		}
		
		// If not, we look for nearby friendly players needing an hand.
		if (targetToHeal == null)
		{
			ArrayList<L2PcInstance> allTargetsNeedingHelp = getTargetsNeedingHelp(900, 75, 75);
			
			if (allTargetsNeedingHelp.size() != 0)
			{
				targetToHeal = allTargetsNeedingHelp.get(0);
				
				amountOfTargetsNeedingHelp += allTargetsNeedingHelp.size();
			}
		}
		
		_assistedTarget = targetToHeal;
		
		// We've got a target to heal.
		if (targetToHeal != null)
		{
			_player.setTarget(targetToHeal);
			
			// Or maybe more than one target. If so, use a massive heal skill.
			if (amountOfTargetsNeedingHelp > Rnd.get(1, 3))
			{
				if (isSkillAvailable(GREATER_GROUP_HEAL_ID))
					return GREATER_GROUP_HEAL_ID;
				else if (isSkillAvailable(CHAIN_HEAL_ID))
					return CHAIN_HEAL_ID;
			}

			boolean canRecoverTargetMana = true;
			
			if (targetToHeal instanceof L2PcInstance)
			{
				final L2PcInstance targetedPlayer = (L2PcInstance) targetToHeal;
				
				if (targetedPlayer.getSkillLevelHash(RECHARGE_ID) != -1)
					canRecoverTargetMana = false;
			}
			
			// If the target has more HP than MP, we recover MP.
			if (canRecoverTargetMana && getHealthPercent(targetToHeal) > getManaPercent(targetToHeal))
			{
				if (isSkillAvailable(RECHARGE_ID))
					return RECHARGE_ID;
			}
			// Otherwise we recover HP.
			else
			{
				if (isSkillAvailable(GREATER_HEAL_ID))
					return GREATER_HEAL_ID;
				else if (isSkillAvailable(MAJOR_HEAL_ID))
					return MAJOR_HEAL_ID;
				else if (isSkillAvailable(BATTLE_HEAL_ID))
					return BATTLE_HEAL_ID;
			}
		}
		else
		{
			// No target to heal.
			// Let's fuck around with an enemy.
			if (Rnd.get(0, 3) == 0 && isSkillAvailable(TRANCE_ID) && target.getFirstEffect(TRANCE_ID) == null)
				return TRANCE_ID;
			
			if (Rnd.get(0, 3) == 0 && isSkillAvailable(MANA_BURN_ID) && target.getFirstEffect(MANA_BURN_ID) == null)
				return MANA_BURN_ID;

			if (target instanceof L2Summon && isSkillAvailable(ERASE_ID))
				return ERASE_ID;
		}
		
		return super.pickSpecialSkill(target);
	}
	
	@Override
	public final int[] getEssentialBuffSkillIds()
	{
		return ESSENTIAL_BUFF_SKILL_IDS;
	}
}