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

package l2server.gameserver.network.serverpackets;

import l2server.gameserver.model.actor.Npc;

/**
 * @author Pere
 */
public final class ExNpcStatus extends L2GameServerPacket {
	private Npc npc;
	
	public ExNpcStatus(Npc npc) {
		this.npc = npc;
	}
	
	@Override
	protected final void writeImpl() {
		writeD(npc.getObjectId());
		writeH(4); // Unk
		writeC(0xff); // Mask
		writeC(npc.isAlikeDead() ? 0x01 : 0x00);
		writeC(npc.isInCombat() ? 0x01 : 0x00);
		writeC(0x00); // Unk
		writeC(0x00); // Unk
		writeC(0x00); // Unk
		writeC(0x00); // Unk
		writeC(0x00); // Unk
		writeC(0x00); // Unk
	}
}
