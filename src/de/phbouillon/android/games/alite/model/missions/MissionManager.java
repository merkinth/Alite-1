package de.phbouillon.android.games.alite.model.missions;

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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import de.phbouillon.android.games.alite.Assets;

public class MissionManager {
	public static final String DIRECTORY_SOUND_MISSION = Assets.DIRECTORY_SOUND + "mission" + File.separator;

	private static final MissionManager instance = new MissionManager();

	// We need to return the set of all stored missions at one point, hence
	// a SparseArray is of no use to us.
	@SuppressLint("UseSparseArrays")
	private final Map <Integer, Mission> missions = new HashMap<>();

	public static MissionManager getInstance() {
		return instance;
	}

	private MissionManager() {
	}

	public void register(Mission m) {
		missions.put(m.getId(), m);
	}

	public Mission get(int id) {
		return missions.get(id);
	}

	public void clear() {
		missions.clear();
	}

	public Collection <Mission> getMissions() {
		return missions.values();
	}
}
