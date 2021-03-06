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

package l2server.gameserver.ai;

import l2server.Config;
import l2server.gameserver.model.L2CharPosition;
import l2server.gameserver.model.Skill;
import l2server.gameserver.model.WorldObject;
import l2server.gameserver.model.actor.Creature;
import l2server.gameserver.model.actor.instance.Player;
import l2server.gameserver.model.actor.instance.StaticObjectInstance;
import l2server.gameserver.templates.skills.SkillTargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static l2server.gameserver.ai.CtrlIntention.*;

public class PlayerAI extends PlayableAI {
	private static Logger log = LoggerFactory.getLogger(PlayerAI.class.getName());

	private boolean thinking; // to prevent recursive thinking

	IntentionCommand nextIntention = null;

	public PlayerAI(Creature creature) {
		super(creature);
	}

	void saveNextIntention(CtrlIntention intention, Object arg0, Object arg1) {
		nextIntention = new IntentionCommand(intention, arg0, arg1);
	}

	@Override
	public IntentionCommand getNextIntention() {
		return nextIntention;
	}

	/**
	 * Saves the current Intention for this PlayerAI if necessary and calls changeIntention in AbstractAI.<BR><BR>
	 *
	 * @param intention The new Intention to set to the AI
	 * @param arg0      The first parameter of the Intention
	 * @param arg1      The second parameter of the Intention
	 */
	@Override
	synchronized void changeIntention(CtrlIntention intention, Object arg0, Object arg1) {
		/*
         if (Config.DEBUG)
		 Logozo.warning("PlayerAI: changeIntention -> " + intention + " " + arg0 + " " + arg1);
		 */

		// do nothing unless CAST intention
		// however, forget interrupted actions when starting to use an offensive skill
		if (intention != AI_INTENTION_CAST || arg0 != null && ((Skill) arg0).isOffensive()) {
			nextIntention = null;
			super.changeIntention(intention, arg0, arg1);
			return;
		}

		// do nothing if next intention is same as current one.
		if (intention == intention && arg0 == intentionArg0 && arg1 == intentionArg1) {
			super.changeIntention(intention, arg0, arg1);
			return;
		}

		// save current intention so it can be used after cast
		saveNextIntention(intention, intentionArg0, intentionArg1);
		super.changeIntention(intention, arg0, arg1);
	}

	/**
	 * Launch actions corresponding to the Event ReadyToAct.<BR><BR>
	 * <p>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Launch actions corresponding to the Event Think</li><BR><BR>
	 */
	@Override
	protected void onEvtReadyToAct() {
		// Launch actions corresponding to the Event Think
		if (nextIntention != null) {
			setIntention(nextIntention.crtlIntention, nextIntention.arg0, nextIntention.arg1);
			nextIntention = null;
		}
		super.onEvtReadyToAct();
	}

	/**
	 * Launch actions corresponding to the Event Cancel.<BR><BR>
	 * <p>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Stop an AI Follow Task</li>
	 * <li>Launch actions corresponding to the Event Think</li><BR><BR>
	 */
	@Override
	protected void onEvtCancel() {
		nextIntention = null;
		super.onEvtCancel();
	}

	/**
	 * Finalize the casting of a skill. This method overrides CreatureAI method.<BR><BR>
	 * <p>
	 * <B>What it does:</B>
	 * Check if actual intention is set to CAST and, if so, retrieves latest intention
	 * before the actual CAST and set it as the current intention for the player
	 */
	@Override
	protected void onEvtFinishCasting() {
		if (getIntention() == AI_INTENTION_CAST) {
			// run interrupted or next intention

			IntentionCommand nextIntention = this.nextIntention;
			if (nextIntention != null) {
				if (nextIntention.crtlIntention != AI_INTENTION_CAST) // previous state shouldn't be casting
				{
					setIntention(nextIntention.crtlIntention, nextIntention.arg0, nextIntention.arg1);
				} else {
					setIntention(AI_INTENTION_IDLE);
				}
			} else {
                /*
				 if (Config.DEBUG)
				 Logozo.warning("PlayerAI: no previous intention set... Setting it to IDLE");
				 */
				// set intention to idle if skill doesn't change intention.
				setIntention(AI_INTENTION_IDLE);
			}
		}
	}

	@Override
	protected void onIntentionRest() {
		if (getIntention() != AI_INTENTION_REST) {
			changeIntention(AI_INTENTION_REST, null, null);
			setTarget(null);
			if (getAttackTarget() != null) {
				setAttackTarget(null);
			}
			clientStopMoving(null);
		}
	}

	@Override
	protected void onIntentionActive() {
		setIntention(AI_INTENTION_IDLE);
	}

	/**
	 * Manage the Move To Intention : Stop current Attack and Launch a Move to Location Task.<BR><BR>
	 * <p>
	 * <B><U> Actions</U> : </B><BR><BR>
	 * <li>Stop the actor auto-attack server side AND client side by sending Server->Client packet AutoAttackStop (broadcast) </li>
	 * <li>Set the Intention of this AI to AI_INTENTION_MOVE_TO </li>
	 * <li>Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast) </li><BR><BR>
	 */
	@Override
	protected void onIntentionMoveTo(L2CharPosition pos) {
		if (getIntention() == AI_INTENTION_REST) {
			// Cancel action client side by sending Server->Client packet ActionFailed to the Player actor
			clientActionFailed();
			return;
		}

		if (actor.isAllSkillsDisabled() || actor.isCastingNow() || actor.isAttackingNow()) {
			clientActionFailed();
			saveNextIntention(AI_INTENTION_MOVE_TO, pos, null);
			return;
		}

		// Set the Intention of this AbstractAI to AI_INTENTION_MOVE_TO
		changeIntention(AI_INTENTION_MOVE_TO, pos, null);

		// Stop the actor auto-attack client side by sending Server->Client packet AutoAttackStop (broadcast)
		clientStopAutoAttack();

		// Abort the attack of the Creature and send Server->Client ActionFailed packet
		actor.abortAttack();

		// Move the actor to Location (x,y,z) server side AND client side by sending Server->Client packet CharMoveToLocation (broadcast)
		moveTo(pos.x, pos.y, pos.z);
	}

	@Override
	protected void clientNotifyDead() {
		clientMovingToPawnOffset = 0;
		clientMoving = false;

		super.clientNotifyDead();
	}

	private void thinkAttack() {
		Creature target = getAttackTarget();
		if (target == null) {
			return;
		}
		if (checkTargetLostOrDead(target)) {
			// Notify the target
			setAttackTarget(null);
			return;
		}
		if (maybeMoveToPawn(target, actor.getPhysicalAttackRange())) {
			return;
		}

		actor.doAttack(target);
	}

	private void thinkCast() {
		Creature target = getCastTarget();
		if (Config.DEBUG) {
			log.warn("PlayerAI: thinkCast -> Start");
		}

		if ((skill.getTargetType() == SkillTargetType.TARGET_GROUND || skill.getTargetType() == SkillTargetType.TARGET_GROUND_AREA) &&
				actor instanceof Player) {
			if (maybeMoveToPosition(actor.getSkillCastPosition(), actor.getMagicalAttackRange(skill))) {
				actor.setCastingNow(false);
				actor.setCastingNow2(false);
				return;
			}
		} else {
			if (checkTargetLost(target)) {
				if (skill.isOffensive() && getAttackTarget() != null) {
					//Notify the target
					setCastTarget(null);
				}
				actor.setCastingNow(false);
				actor.setCastingNow2(false);
				return;
			}
			if (target != null && maybeMoveToPawn(target, actor.getMagicalAttackRange(skill))) {
				actor.setCastingNow(false);
				actor.setCastingNow2(false);
				return;
			}
		}

		if (skill.getHitTime() > 50 && !skill.isSimultaneousCast()) {
			clientStopMoving(null);
		}

		WorldObject oldTarget = actor.getTarget();
		if (oldTarget != null && target != null && oldTarget != target) {
			// Replace the current target by the cast target
			actor.setTarget(getCastTarget());
			// Launch the Cast of the skill
			actor.doCast(skill, actor.canDoubleCast() && !actor.wasLastCast1());
			// Restore the initial target
			actor.setTarget(oldTarget);
		} else {
			actor.doCast(skill, actor.canDoubleCast() && !actor.wasLastCast1());
		}
	}

	private void thinkPickUp() {
		if (actor.isAllSkillsDisabled() || actor.isCastingNow()) {
			return;
		}
		WorldObject target = getTarget();
		if (checkTargetLost(target)) {
			return;
		}
		if (maybeMoveToPawn(target, 36)) {
			return;
		}
		setIntention(AI_INTENTION_IDLE);
		((Player) actor).doPickupItem(target);
	}

	private void thinkInteract() {
		if (actor.isAllSkillsDisabled() || actor.isCastingNow()) {
			return;
		}
		WorldObject target = getTarget();
		if (checkTargetLost(target)) {
			return;
		}
		if (maybeMoveToPawn(target, 36)) {
			return;
		}
		if (!(target instanceof StaticObjectInstance)) {
			((Player) actor).doInteract((Creature) target);
		}
		setIntention(AI_INTENTION_IDLE);
	}

	@Override
	protected void onEvtThink() {
		if (thinking && getIntention() != AI_INTENTION_CAST) // casting must always continue
		{
			return;
		}
		
		/*
		 if (Config.DEBUG)
		 Logozo.warning("PlayerAI: onEvtThink -> Check intention");
		 */

		thinking = true;
		try {
			if (getIntention() == AI_INTENTION_ATTACK) {
				thinkAttack();
			} else if (getIntention() == AI_INTENTION_CAST) {
				thinkCast();
			} else if (getIntention() == AI_INTENTION_PICK_UP) {
				thinkPickUp();
			} else if (getIntention() == AI_INTENTION_INTERACT) {
				thinkInteract();
			}
		} finally {
			thinking = false;
		}
	}
}
