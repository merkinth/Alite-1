package de.phbouillon.android.games.alite.screens.opengl.ingame;

/* Alite - Discover the Universe on your Favorite Android Device
 * Copyright (C) 2015 Philipp Bouillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful and
 * fun, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see
 * http://http://www.gnu.org/licenses/gpl-3.0.txt.
 */

import de.phbouillon.android.framework.IMethodHook;
import de.phbouillon.android.games.alite.L;
import de.phbouillon.android.games.alite.R;

class CloakingEvent extends TimedEvent {
	private static final long serialVersionUID = 4375455573382798491L;

	CloakingEvent(InGameManager inGame) {
		super(359281437L);
		inGame.getMessage().repeatText(L.string(R.string.msg_cloaking_active), 3);
		addAlarmEvent(new IMethodHook() {
			private static final long serialVersionUID = -4029522005378301546L;
			@Override
			public void execute(float deltaTime) {
				inGame.reduceShipEnergy(1);
			}
		});
	}

}
