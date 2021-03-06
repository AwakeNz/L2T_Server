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

package l2server.gameserver.network.clientpackets;

import l2server.Config;
import l2server.gameserver.model.actor.instance.Player;
import l2server.gameserver.network.serverpackets.ExConfirmAddingContact;

/**
 * Format: (ch)S
 * S: Character Name
 *
 * @author UnAfraid & mrTJO
 */
public class RequestExAddContactToContactList extends L2GameClientPacket {
	private String name;
	
	@Override
	protected void readImpl() {
		name = readS();
	}
	
	@Override
	protected void runImpl() {
		if (!Config.ALLOW_MAIL) {
			return;
		}
		
		if (name == null) {
			return;
		}
		
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null) {
			return;
		}
		
		boolean charAdded = activeChar.getContactList().add(name);
		activeChar.sendPacket(new ExConfirmAddingContact(name, charAdded));
	}
}
