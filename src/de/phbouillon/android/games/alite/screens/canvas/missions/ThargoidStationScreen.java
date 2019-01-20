package de.phbouillon.android.games.alite.screens.canvas.missions;

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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import android.media.MediaPlayer;
import de.phbouillon.android.framework.Graphics;
import de.phbouillon.android.games.alite.Alite;
import de.phbouillon.android.games.alite.AliteLog;
import de.phbouillon.android.games.alite.Assets;
import de.phbouillon.android.games.alite.ScreenCodes;
import de.phbouillon.android.games.alite.colors.ColorScheme;
import de.phbouillon.android.games.alite.model.Player;
import de.phbouillon.android.games.alite.model.missions.MissionManager;
import de.phbouillon.android.games.alite.model.missions.ThargoidStationMission;
import de.phbouillon.android.games.alite.screens.canvas.AliteScreen;
import de.phbouillon.android.games.alite.screens.canvas.TextData;

//This screen never needs to be serialized, as it is not part of the InGame state.
public class ThargoidStationScreen extends AliteScreen {
	private final MediaPlayer mediaPlayer;

	private final String missionDescription =
			"The Galactic Intelligence Agency reports that a nearby system " +
			"has been invaded by Thargoid ships and that the space station " +
			"has been captured. All available forces are being gathered to " +
			"destroy the invading force. The space station has become the " +
			"main Thargoid headquarters and it's from this base that the " +
			"planetary attack is being mounted. The space station must be " +
			"destroyed - no matter what the cost!";

	private final String attentionCommander = "Attention Commander!";

	private final String success =
			"Thanks for destroying the Thargoid base. A new device was " +
			"salvaged from one of the Thargoid ships which we have fitted " +
			"to your ship. The device appears to jam ECM broadcasts.";

	private MissionLine attCommander;
	private MissionLine missionLine;
	private int lineIndex = 0;
	private TextData[] missionText;
	private final int givenState;

	public ThargoidStationScreen(Alite game, int state) {
		super(game);
		givenState = state;
		ThargoidStationMission mission = (ThargoidStationMission) MissionManager.getInstance().get(ThargoidStationMission.ID);
		mediaPlayer = new MediaPlayer();
		String path = MissionManager.DIRECTORY_SOUND_MISSION + "5/";
		try {
			attCommander = new MissionLine(path + "01.mp3", attentionCommander);
			if (state == 0) {
				missionLine = new MissionLine(path + "02.mp3", missionDescription);
				mission.setPlayerAccepts(true);
				mission.setTarget(game.getGenerator().getCurrentSeed(), game.getPlayer().getCurrentSystem().getIndex(), 1);
			} else if (state == 1) {
				missionLine = new MissionLine(path + "04.mp3", success);
			 	mission.onMissionComplete();
				Player player = game.getPlayer();
				player.removeActiveMission(mission);
				player.addCompletedMission(mission);
				player.resetIntergalacticJumpCounter();
				player.resetJumpCounter();
			} else {
				AliteLog.e("Unknown State", "Invalid state variable has been passed to ThargoidStationScreen: " + state);
			}
		} catch (IOException e) {
			AliteLog.e("Error reading mission", "Could not read mission audio.", e);
		}
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);
		if (lineIndex == 0 && !attCommander.isPlaying()) {
			attCommander.play(mediaPlayer);
			lineIndex++;
		} else if (lineIndex == 1 && !attCommander.isPlaying() && !missionLine.isPlaying()) {
			missionLine.play(mediaPlayer);
			lineIndex++;
		}
	}

	@Override
	public void present(float deltaTime) {
		Graphics g = game.getGraphics();
		g.clear(ColorScheme.get(ColorScheme.COLOR_BACKGROUND));
		displayTitle("Mission #5 - Destroy the Alien Space Station");

		g.drawText(attentionCommander, 50, 200, ColorScheme.get(ColorScheme.COLOR_INFORMATION_TEXT), Assets.regularFont);
		if (missionText != null) {
			displayText(g, missionText);
		}
	}

	@Override
	public void activate() {
		missionText = computeTextDisplay(game.getGraphics(), missionLine.getText(), 50, 300, 800, 40, ColorScheme.get(ColorScheme.COLOR_MAIN_TEXT));
	}

	public static boolean initialize(Alite alite, DataInputStream dis) {
		try {
			int state = dis.readInt();
			alite.setScreen(new ThargoidStationScreen(alite, state));
		} catch (IOException e) {
			AliteLog.e("ThargoidStation Screen Initialize", "Error in initializer.", e);
			return false;
		}
		return true;
	}

	@Override
	public void saveScreenState(DataOutputStream dos) throws IOException {
		dos.writeInt(givenState);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (mediaPlayer != null) {
			mediaPlayer.reset();
		}
	}

	@Override
	public void pause() {
		super.pause();
		if (mediaPlayer != null) {
			mediaPlayer.reset();
		}
	}

	@Override
	public int getScreenCode() {
		return ScreenCodes.THARGOID_STATION_SCREEN;
	}
}
