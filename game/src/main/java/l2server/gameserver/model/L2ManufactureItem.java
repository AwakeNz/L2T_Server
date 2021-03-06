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

package l2server.gameserver.model;

import l2server.gameserver.RecipeController;

/**
 * This class ...
 *
 * @version $Revision: 1.1.2.2.2.1 $ $Date: 2005/03/27 15:29:32 $
 */
public class L2ManufactureItem {
	private int recipeId;
	private long cost;
	private boolean isDwarven;
	
	public L2ManufactureItem(int recipeId, long cost) {
		this.recipeId = recipeId;
		this.cost = cost;
		
		isDwarven = RecipeController.getInstance().getRecipeList(recipeId).isDwarvenRecipe();
	}
	
	public int getRecipeId() {
		return recipeId;
	}
	
	public long getCost() {
		return cost;
	}
	
	public boolean isDwarven() {
		return isDwarven;
	}
}
