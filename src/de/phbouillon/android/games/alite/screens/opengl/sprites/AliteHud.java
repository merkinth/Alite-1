package de.phbouillon.android.games.alite.screens.opengl.sprites;

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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import android.opengl.GLES11;
import de.phbouillon.android.framework.Input.TouchEvent;
import de.phbouillon.android.framework.Timer;
import de.phbouillon.android.framework.impl.gl.Sprite;
import de.phbouillon.android.games.alite.*;
import de.phbouillon.android.games.alite.colors.AliteColor;
import de.phbouillon.android.games.alite.model.Laser;
import de.phbouillon.android.games.alite.model.PlayerCobra;
import de.phbouillon.android.games.alite.screens.opengl.ingame.InGameManager;

public class AliteHud extends Sprite implements Serializable {
	private static final long serialVersionUID      = -1218984695547293867L;
	private static final long ENEMY_VISIBLE_PHASE   = 660; // ms
	private static final long ENEMY_INVISIBLE_PHASE = 340; // ms

	public static final int MAX_DISTANCE = 44000;
	public static final int MAX_DISTANCE_SQ = MAX_DISTANCE * MAX_DISTANCE;
	public static final int MAXIMUM_OBJECTS = 128;
	public static final int RADAR_X1 = 558;
	public static final int RADAR_Y1 = 700;
	public static final int RADAR_X2 = RADAR_X1 + 803;
	public static final int RADAR_Y2 = RADAR_Y1 + 303;

	public static final int ALITE_TEXT_X1 = 832;
	public static final int ALITE_TEXT_Y1 = 1020;
	public static final int ALITE_TEXT_X2 = ALITE_TEXT_X1 + 256;
	public static final int ALITE_TEXT_Y2 = ALITE_TEXT_Y1 + 60;

	private transient Sprite lollipop = new Sprite(0,0,0,0,0,0,1,1,"");
	private float [][] objects = new float[MAXIMUM_OBJECTS][3];
	private int[] objectColors = new int[MAXIMUM_OBJECTS];
	private boolean [] enemy = new boolean[MAXIMUM_OBJECTS];
	private boolean [] enabled = new boolean[MAXIMUM_OBJECTS];
	private final Sprite laser;
	private final Sprite aliteText;
	private final Sprite safeIcon;
	private final Sprite ecmIcon;
	private final Sprite frontViewport;
	private final Sprite rearViewport;
	private final Sprite leftViewport;
	private final Sprite rightViewport;
	private boolean enemiesVisible = true;

	private int viewDirection = 0;
	private int currentLaserIndex = 0;

	static final String TEXTURE_FILE = "textures/radar_final.png";

	private final InfoGaugeRenderer infoGauges;
	private final CompassRenderer compass;
	private float zoomFactor = 1.0f;
	private Timer ecmActive;
	private boolean witchSpace = false;
	private ControlPad controlPad;
	private CursorKeys controlKeys;
	private final Timer timer = new Timer().setAutoResetWithSkipFirstCall();

	public AliteHud() {
		super(RADAR_X1, RADAR_Y1, RADAR_X2, RADAR_Y2, 0, 0, 1, 1, TEXTURE_FILE);
		SpriteData spriteData = Alite.get().getTextureManager().getSprite(TEXTURE_FILE, "radar");
		setTextureCoords(spriteData.x, spriteData.y, spriteData.x2, spriteData.y2);

		laser         = genSprite("pulse_laser", 896, 476);
		aliteText     = genSprite("alite", 864, 1030);
		safeIcon      = genSprite("s", 1284, RADAR_Y2 - 68);
		ecmIcon       = genSprite("e", RADAR_X1 - 40, RADAR_Y2 - 68);
		frontViewport = genSprite("front", (RADAR_X2 + RADAR_X1 >> 1) - 133, RADAR_Y1);
		rearViewport  = genSprite("rear",  (RADAR_X2 + RADAR_X1 >> 1) - 133, RADAR_Y2 + RADAR_Y1 >> 1);
		leftViewport  = genSprite("left", RADAR_X1, (RADAR_Y2 + RADAR_Y1 >> 1) - 73);
		rightViewport = genSprite("right", RADAR_X2 + RADAR_X1 >> 1, (RADAR_Y2 + RADAR_Y1 >> 1) - 73);
		infoGauges    = new InfoGaugeRenderer(this);
		compass       = new CompassRenderer(this);
		if (Settings.controlMode == ShipControl.CONTROL_PAD) {
			controlPad = new ControlPad();
		} else if (Settings.controlMode == ShipControl.CURSOR_BLOCK) {
			controlKeys = new CursorKeys(false);
		} else if (Settings.controlMode == ShipControl.CURSOR_SPLIT_BLOCK) {
			controlKeys = new CursorKeys(true);
		}
	}

	private void readObject(ObjectInputStream in) throws IOException {
		try {
			AliteLog.d("readObject", "AliteHud.readObject");
			in.defaultReadObject();
			AliteLog.d("readObject", "AliteHud.readObject I");
			lollipop = new Sprite(0,0,0,0,0,0,1,1,"");
			Alite.get().getTextureManager().addTexture(TEXTURE_FILE);
			SpriteData spriteData = Alite.get().getTextureManager().getSprite(TEXTURE_FILE, "radar");
			setTextureCoords(spriteData.x, spriteData.y, spriteData.x2, spriteData.y2);
			currentLaserIndex = -1;
			computeLaser();
			AliteLog.d("readObject", "AliteHud.readObject II");
		} catch (ClassNotFoundException e) {
			AliteLog.e("Class not found", e.getMessage(), e);
		}
	}

	Sprite genSprite(String name, int x, int y) {
		Alite alite = Alite.get();
		SpriteData spriteData = alite.getTextureManager().getSprite(TEXTURE_FILE, name);
		return new Sprite(x, y, x + spriteData.origWidth, y + spriteData.origHeight,
			spriteData.x, spriteData.y, spriteData.x2, spriteData.y2, TEXTURE_FILE);
	}

	private void computeLaser() {
		Laser laser = null;
		Alite alite = Alite.get();
		switch (viewDirection) {
			case 0: laser = alite.getPlayer().getCobra().getLaser(PlayerCobra.DIR_FRONT); break;
			case 1: laser = alite.getPlayer().getCobra().getLaser(PlayerCobra.DIR_RIGHT); break;
			case 2: laser = alite.getPlayer().getCobra().getLaser(PlayerCobra.DIR_REAR);  break;
			case 3: laser = alite.getPlayer().getCobra().getLaser(PlayerCobra.DIR_LEFT);  break;
		}
		if (laser != null) {
			if (laser.getIndex() != currentLaserIndex) {
				SpriteData spriteData =  alite.getTextureManager().getSprite(TEXTURE_FILE,
						laser.getIndex() == 0 ? "pulse_laser" :
						laser.getIndex() == 1 ? "beam_laser" :
						laser.getIndex() == 2 ? "mining_laser" :
							"military_laser");
				currentLaserIndex = laser.getIndex();
				this.laser.setTextureCoords(spriteData.x, spriteData.y, spriteData.x2, spriteData.y2);
			}
		} else {
			currentLaserIndex = -1;
		}
	}

	public void setObject(int index, float x, float y, float z, int color, boolean isEnemy) {
		if (index >= MAXIMUM_OBJECTS) {
			AliteLog.d("ALITE Hud", "Maximum number of HUD objects exceeded!");
			return;
		}
		objects[index][0] = x;
		objects[index][1] = y;
		objects[index][2] = z;
		objectColors[index] = color;
		enemy[index] = isEnemy;
		enabled[index] = true;
	}

	public int zoomIn() {
		if (zoomFactor > 3.0f) {
			return 4;
		}
		if (Math.abs(zoomFactor - 1.0f) < 0.001f) {
			zoomFactor = 2.0f;
			return 2;
		}
		zoomFactor = 4.0f;
		return 4;
	}

	public int zoomOut() {
		if (zoomFactor < 1.5f) {
			return 1;
		}
		if (Math.abs(zoomFactor - 2.0f) < 0.001f) {
			zoomFactor = 1.0f;
			return 1;
		}
		zoomFactor = 2.0f;
		return 2;
	}

	public void setZoomFactor(float newZoomFactor) {
		zoomFactor = newZoomFactor;
	}

	public float getZoomFactor() {
		return zoomFactor;
	}

	public void clear() {
		for (int i = 0; i < MAXIMUM_OBJECTS; i++) {
			enabled[i] = false;
		}
	}

	public void setPlanet(float x, float y, float z) {
		compass.setPlanet(x, y, z);
	}

	public void disableObject(int index) {
		if (index >= MAXIMUM_OBJECTS) {
			return;
		}
		enabled[index] = false;
	}

	private void renderLollipops() {
		float x;
		float y1;
		float y2;
		for (int i = 0; i < MAXIMUM_OBJECTS; i++) {
			if (enabled[i] && (!enemy[i] || enemiesVisible)) {
				x = objects[i][0] / (110.0f / zoomFactor) + RADAR_X1 + 402;
				y1 = objects[i][2] / (294.0f / zoomFactor) + RADAR_Y1 + 146;
				y2 = y1 - objects[i][1] / (294.0f / zoomFactor);
				float distance = objects[i][0] * objects[i][0] + objects[i][1] * objects[i][1] + objects[i][2] * objects[i][2];
				if (distance > 44100.0f / zoomFactor * 44100.0f / zoomFactor) {
					// Don't show objects farther away than 44,100m
					continue;
				}

				Alite.get().getGraphics().setColor(objectColors[i], Settings.alpha);
				lollipop.setPosition(x - 13, y2, x + 12, y2 + 15);
				lollipop.simpleRender(); // bar

				if (Math.abs(y2 - y1) > 15.0f) {
					lollipop.setPosition(x - 13, y1 < y2 ? y1 + 15 : y1, x - 3, y1 < y2 ? y2 : y2 + 15);
					lollipop.simpleRender(); // stem
				}
			}
		}
	}

	public boolean isWitchSpace() {
		return witchSpace;
	}

	public void setWitchSpace(boolean b) {
		witchSpace = b;
	}

	public void setViewDirection(int viewDirection) {
		if (viewDirection != this.viewDirection) {
			this.viewDirection = viewDirection;
			computeLaser();
		}
	}

	public void showECM() {
		if (ecmActive == null) {
			ecmActive = new Timer().setAutoReset();
		} else {
			ecmActive.reset();
		}
	}

	public float getY() {
		return controlPad != null ? controlPad.getY() : controlKeys != null ? controlKeys.getY() : 0.0f;
	}

	public float getZ() {
		return controlPad != null ? controlPad.getZ() : controlKeys != null ? controlKeys.getZ() : 0.0f;
	}

	public void update(float deltaTime) {
		if (enemiesVisible && timer.hasPassedMillis(ENEMY_VISIBLE_PHASE)) {
			enemiesVisible = false;
		} else if (!enemiesVisible && timer.hasPassedMillis(ENEMY_INVISIBLE_PHASE)) {
			enemiesVisible = true;
		}
		if (controlPad != null) {
			controlPad.update(deltaTime);
		}
		if (controlKeys != null) {
			controlKeys.update(deltaTime);
		}
	}

	public boolean handleUI(TouchEvent event) {
		if (controlPad != null) {
			return controlPad.handleUI(event);
		}
		if (controlKeys != null) {
			return controlKeys.handleUI(event);
		}
		return false;
	}

	public void mapDirections(boolean left, boolean right, boolean up, boolean down) {
		boolean u = Settings.reversePitch ? down : up;
		boolean d = Settings.reversePitch ? up : down;

		if (controlPad != null) {
			controlPad.setActiveIndex(left, right, u, d);
		}
		if (controlKeys != null) {
			controlKeys.setHighlight(left, right, u, d);
		}
	}

	@Override
	public void render() {
		setUp();
		GLES11.glColor4f(Settings.alpha, Settings.alpha, Settings.alpha, Settings.alpha);
		GLES11.glDrawArrays(GLES11.GL_TRIANGLE_STRIP, 0, 4);

		computeLaser();
		if (currentLaserIndex >= 0) {
			laser.simpleRender();
		}
		if (!witchSpace) {
			compass.render();
		}

		infoGauges.render();
		GLES11.glColor4f(Settings.alpha, Settings.alpha, Settings.alpha, Settings.alpha);
		aliteText.justRender();
		if (InGameManager.playerInSafeZone) {
			safeIcon.justRender();
		}
		if (ecmActive != null && !ecmActive.hasPassedSeconds(6)) {
			ecmIcon.justRender();
		} else{
			ecmActive = null;
		}

		if (Settings.controlMode == ShipControl.CONTROL_PAD && controlPad != null) {
			controlPad.render();
		} else if ((Settings.controlMode == ShipControl.CURSOR_BLOCK || Settings.controlMode == ShipControl.CURSOR_SPLIT_BLOCK) && controlKeys != null) {
			controlKeys.render();
		}

		GLES11.glBlendFunc(GLES11.GL_ONE, GLES11.GL_ONE);
		switch (viewDirection) {
			case 0: frontViewport.justRender(); break;
			case 1: rightViewport.justRender(); break;
			case 2: rearViewport.justRender();  break;
			case 3: leftViewport.justRender();  break;
		}

		GLES11.glDisableClientState(GLES11.GL_TEXTURE_COORD_ARRAY);
		GLES11.glBindTexture(GLES11.GL_TEXTURE_2D, 0);
		renderLollipops();
		if (zoomFactor > 1.5f && zoomFactor < 3.0f) {
			Alite.get().getGraphics().drawText(L.string(R.string.radar_zoom_x2), RADAR_X1 + 20, RADAR_Y1 + 20,
				AliteColor.argb(0.6f * Settings.alpha, 0.94f * Settings.alpha, 0.94f * Settings.alpha, 0.0f),
				Assets.regularFont, 1.0f);
		} else if (zoomFactor > 3.0f) {
			Alite.get().getGraphics().drawText(L.string(R.string.radar_zoom_x4), RADAR_X1 + 20, RADAR_Y1 + 20,
				AliteColor.argb(0.6f * Settings.alpha, 0.94f * Settings.alpha, 0.94f * Settings.alpha, 0.0f),
				Assets.regularFont, 1.0f);
		}
		GLES11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		GLES11.glDisable(GLES11.GL_BLEND);
		GLES11.glEnable(GLES11.GL_CULL_FACE);
		GLES11.glEnable(GLES11.GL_LIGHTING);

		GLES11.glDisableClientState(GLES11.GL_VERTEX_ARRAY);
		cleanUp();

	}

	public boolean isTargetInCenter() {
		return compass != null && compass.isTargetInCenter();
	}
}
