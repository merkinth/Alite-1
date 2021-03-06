package de.phbouillon.android.games.alite.screens.canvas;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.opengl.GLES11;
import android.text.InputFilter;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import de.phbouillon.android.framework.Graphics;
import de.phbouillon.android.framework.Input.TouchEvent;
import de.phbouillon.android.framework.Pixmap;
import de.phbouillon.android.framework.Screen;
import de.phbouillon.android.framework.impl.gl.GlUtils;
import de.phbouillon.android.framework.impl.gl.font.GLText;
import de.phbouillon.android.games.alite.*;
import de.phbouillon.android.games.alite.colors.AliteColor;
import de.phbouillon.android.games.alite.colors.ColorScheme;
import de.phbouillon.android.games.alite.screens.opengl.ingame.FlightScreen;
import de.phbouillon.android.games.alite.screens.opengl.objects.AliteObject;

//This screen never needs to be serialized, as it is not part of the InGame state.
public abstract class AliteScreen extends Screen {
	private final float[] lightAmbient  = { 0.5f, 0.5f, 0.7f, 1.0f };
	private final float[] lightDiffuse  = { 0.4f, 0.4f, 0.8f, 1.0f };
	private final float[] lightSpecular = { 0.5f, 0.5f, 1.0f, 1.0f };
	private final float[] lightPosition = { 100.0f, 30.0f, -10.0f, 1.0f };
	private final float[] sunLightAmbient  = {1.0f, 1.0f, 1.0f, 1.0f};
	private final float[] sunLightDiffuse  = {1.0f, 1.0f, 1.0f, 1.0f};
	private final float[] sunLightSpecular = {1.0f, 1.0f, 1.0f, 1.0f};
	private final float[] sunLightPosition = {0.0f, 0.0f, 0.0f, 1.0f};

	private boolean popupWindow;
	private String message;
	private int maxLength;
	private int dialogState;
	private Button ok;
	private Button yes;
	private Button no;
	protected Screen newScreen;
	protected int messageResult;
	protected String inputText;
	protected transient Alite game;
	private transient EditText editText;
	protected final Map<String, Pixmap> pics = new HashMap<>();

	private static final int DIALOG_BUTTON_MASK = 7;
	private static final int DIALOG_OK = 0;
	private static final int DIALOG_YES_NO = 1;

	private static final int DIALOG_TYPE_MASK = 56;
	private static final int DIALOG_NORMAL = 0;
	private static final int DIALOG_LARGE = 8;
	private static final int DIALOG_INPUT_POPUP = 16;

	private static final int DIALOG_MODAL = 128;
	private static final int DIALOG_VISIBLE = 256;
	private static final int DIALOG_RENDERING = 512; // to avoid calling disposing dialog during rendering

	protected static final int RESULT_NONE = 0;
	protected static final int RESULT_YES = 1;
	protected static final int RESULT_NO = -1;

	public AliteScreen() {
		game = Alite.get();
		setUpForDisplay();
	}

	public void showMessageDialog(String message) {
		setMessageDialog(message, DIALOG_OK);
	}

	public void showLargeMessageDialog(String message) {
		setMessageDialog(message, DIALOG_OK | DIALOG_LARGE);
	}

	protected void showQuestionDialog(String message) {
		setMessageDialog(message, DIALOG_YES_NO | DIALOG_NORMAL);
	}

	private void setMessageDialog(String message, int dialogState) {
		this.message = message;
		this.dialogState = dialogState;
		popupWindow = false;
	}

	void showModalQuestionDialog(String message) {
		setMessageDialog(message, DIALOG_YES_NO | DIALOG_NORMAL | DIALOG_MODAL);
	}

	void showLargeModalQuestionDialog(String message) {
		setMessageDialog(message, DIALOG_YES_NO | DIALOG_LARGE | DIALOG_MODAL);
	}

	boolean isMessageDialogActive() {
		return message != null;
	}

	void popupTextInput(String message, String inputText, int maxLength) {
		setMessageDialog(message, DIALOG_YES_NO | DIALOG_INPUT_POPUP | DIALOG_MODAL);
		this.inputText = inputText;
		this.maxLength = maxLength;
		popupWindow = true;
	}

	protected void centerText(String text, int y, GLText f, int color) {
		centerText(text, 0, AliteConfig.DESKTOP_WIDTH, y, f, color);
	}

	void centerText(String text, int minX, int maxX, int y, GLText f, int color) {
		int center = maxX + minX - game.getGraphics().getTextWidth(text, f) >> 1;
		game.getGraphics().drawText(text, center, y, color, f);
	}

	void centerTextWide(String text, int y, GLText f, int color) {
		centerText(text, 0, AliteConfig.SCREEN_WIDTH, y, f, color);
	}

	protected void displayTitle(String title) {
		displayTitle(title, AliteConfig.DESKTOP_WIDTH);
	}

	private void displayTitle(String title, int width) {
		Graphics g = game.getGraphics();
		g.verticalGradientRect(0, 0, width - 1, 80,
			ColorScheme.get(ColorScheme.COLOR_BACKGROUND_DARK), ColorScheme.get(ColorScheme.COLOR_BACKGROUND_LIGHT));
		g.drawPixmap(Assets.aliteLogoSmall, 20, 5);
		g.drawPixmap(Assets.aliteLogoSmall, width - 120, 5);
		centerText(title, 60, Assets.titleFont, ColorScheme.get(ColorScheme.COLOR_MESSAGE));
	}

	void displayWideTitle(String title) {
		displayTitle(title, AliteConfig.SCREEN_WIDTH);
	}

	private void addInputLayout(int x, int y, int width, int height) {
		game.runOnUiThread(() -> {
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width - 20 - x, height);
			layoutParams.setMargins(x, y, 0, 0);
			editText = new EditText(game);
			game.addContentView(editText,layoutParams);

			editText.setTextColor(ColorScheme.get(ColorScheme.COLOR_MAIN_TEXT));
			editText.setBackgroundColor(ColorScheme.get(ColorScheme.COLOR_TEXT_AREA_BACKGROUND));
			editText.setTextSize(20);
			editText.setTypeface(null, Typeface.BOLD);
			editText.setSingleLine();
			editText.setCursorVisible(true);
			editText.setLongClickable(false);
			editText.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN);
			editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			editText.setPadding(2, 2, 2, 2);
			editText.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
				public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
					return false;
				}
				public void onDestroyActionMode(ActionMode mode) {
				}
				public boolean onCreateActionMode(ActionMode mode, Menu menu) {
					return false;
				}
				public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
					return false;
				}
			});

			GradientDrawable gd = new GradientDrawable();
			gd.setColor(ColorScheme.get(ColorScheme.COLOR_TEXT_AREA_BACKGROUND));
			gd.setStroke(0, 0);
			editText.setBackgroundDrawable(gd);
			editText.setOnEditorActionListener((v, actionId, event) -> {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					dialogEventsProcessed(null, RESULT_YES);
				}
				return true;
			});
			editText.setText(inputText);
			if (maxLength >= 0) {
				editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
			}
			editText.requestFocus();
			((InputMethodManager) game.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(editText, 0);
		});
	}

	private Rect getDialogRect() {
		int width = (dialogState & DIALOG_TYPE_MASK) == DIALOG_NORMAL ? 750 :
			(dialogState & DIALOG_TYPE_MASK) == DIALOG_LARGE ? 1000 : 1700;
		int height = (dialogState & DIALOG_TYPE_MASK) == DIALOG_NORMAL ? 400 :
			(dialogState & DIALOG_TYPE_MASK) == DIALOG_LARGE ? 600 : 140;
		int x = AliteConfig.DESKTOP_WIDTH - width >> 1;
		int y = (dialogState & DIALOG_TYPE_MASK) == DIALOG_INPUT_POPUP ? 100 : AliteConfig.SCREEN_HEIGHT - height >> 1;
		return new Rect(x, y, x + width - 1, y + height - 1);
	}

	private void renderMessage() {
		if (!isMessageDialogActive() || (dialogState & DIALOG_RENDERING) != 0) {
			return;
		}
		dialogState |= DIALOG_RENDERING;
		Rect r = getDialogRect();
		int width = r.right - r.left + 1;
		int height = r.bottom - r.top + 1;
		int buttonSize = 110;
		int buttonGap = 20;
		final int BORDER_GAP = 15;
		Graphics g = game.getGraphics();
		g.verticalGradientRect(r.left, r.top, width, height,
			ColorScheme.get(ColorScheme.COLOR_BACKGROUND_LIGHT), ColorScheme.get(ColorScheme.COLOR_BACKGROUND_DARK));
		g.rec3d(r.left, r.top, width, height,5,
			AliteColor.lighten(ColorScheme.get(ColorScheme.COLOR_BACKGROUND_LIGHT), 0.1),
			AliteColor.lighten(ColorScheme.get(ColorScheme.COLOR_BACKGROUND_DARK), -0.1));

		displayText(g, computeTextDisplay(game.getGraphics(), message, r.left + 20, r.top + 70 + (popupWindow ? 20 : 0),
			new int[] { width - 40 }, 60, ColorScheme.get(ColorScheme.COLOR_MESSAGE),
			(dialogState & DIALOG_TYPE_MASK) == DIALOG_INPUT_POPUP || (dialogState & DIALOG_MODAL) == 0 ?
				Assets.titleFont : Assets.regularFont, false));
		if ((dialogState & DIALOG_BUTTON_MASK) == DIALOG_OK) {
			if (ok == null) {
				ButtonRegistry.get().setNextAsMessageButton();
				ok = Button.createGradientPictureButton(r.left + width - buttonSize - BORDER_GAP,
					r.top + height - buttonSize - BORDER_GAP, buttonSize, buttonSize, Assets.yesIcon)
					.setPixmapOffset(buttonSize - 100 >> 1, buttonSize - 100 >> 1)
					.setCommand(RESULT_NONE);
			}
			ok.render(g);
			dialogState|= DIALOG_VISIBLE;
			dialogState &= ~DIALOG_RENDERING;
			return;
		}

		if (yes == null) {
			ButtonRegistry.get().setNextAsMessageButton();
			yes = Button.createGradientPictureButton(r.left + width - BORDER_GAP - 2 * buttonSize - buttonGap,
				r.top + height - BORDER_GAP - buttonSize, buttonSize, buttonSize, Assets.yesIcon)
				.setPixmapOffset(buttonSize - 100 >> 1, buttonSize - 100 >> 1)
				.setCommand(RESULT_YES);
		}
		if (no == null) {
			ButtonRegistry.get().setNextAsMessageButton();
			no = Button.createGradientPictureButton(r.left + width - BORDER_GAP - buttonSize,
				r.top + height - BORDER_GAP - buttonSize, buttonSize, buttonSize, Assets.noIcon)
				.setPixmapOffset(buttonSize - 100 >> 1, buttonSize - 100 >> 1)
				.setCommand(RESULT_NO);
		}
		yes.render(g);
		no.render(g);

		// Method addInputLayout shows soft input keyboard, hence, an immediate pressing of enter key triggers
		// event dialogEventsProcessed which sets messages and buttons (ok, no, yes) tag variables to null
		// during the presenting process. To avoid null pointer exception calling addInputLayout is placed
		// to the end of this method.
		if (popupWindow && (dialogState & DIALOG_VISIBLE) == 0) {
			addInputLayout(g.transX(r.left + 40 + g.getTextWidth(message, Assets.titleFont)),
				g.transY(r.top + 20), g.transX(width - 2 * buttonSize - buttonGap - BORDER_GAP), g.transY(100));
		}

		dialogState &= ~DIALOG_RENDERING;
		dialogState|= DIALOG_VISIBLE;
	}

	protected void initGl() {
		GlUtils.setViewport(game);
		GLES11.glDisable(GLES11.GL_FOG);
		GLES11.glPointSize(1.0f);
		GLES11.glLineWidth(1.0f);

		GLES11.glBlendFunc(GLES11.GL_ONE, GLES11.GL_ONE_MINUS_SRC_ALPHA);
		GLES11.glDisable(GLES11.GL_BLEND);

		GLES11.glMatrixMode(GLES11.GL_PROJECTION);
		GLES11.glLoadIdentity();
		GlUtils.gluPerspective(game, 45.0f, 1.0f, 900000.0f);
		GLES11.glMatrixMode(GLES11.GL_MODELVIEW);
		GLES11.glLoadIdentity();

		GLES11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GLES11.glShadeModel(GLES11.GL_SMOOTH);

		GLES11.glLightfv(GLES11.GL_LIGHT1, GLES11.GL_AMBIENT, lightAmbient, 0);
		GLES11.glLightfv(GLES11.GL_LIGHT1, GLES11.GL_DIFFUSE, lightDiffuse, 0);
		GLES11.glLightfv(GLES11.GL_LIGHT1, GLES11.GL_SPECULAR, lightSpecular, 0);
		GLES11.glLightfv(GLES11.GL_LIGHT1, GLES11.GL_POSITION, lightPosition, 0);
		GLES11.glEnable(GLES11.GL_LIGHT1);

		GLES11.glLightfv(GLES11.GL_LIGHT2, GLES11.GL_AMBIENT, sunLightAmbient, 0);
		GLES11.glLightfv(GLES11.GL_LIGHT2, GLES11.GL_DIFFUSE, sunLightDiffuse, 0);
		GLES11.glLightfv(GLES11.GL_LIGHT2, GLES11.GL_SPECULAR, sunLightSpecular, 0);
		GLES11.glLightfv(GLES11.GL_LIGHT2, GLES11.GL_POSITION, sunLightPosition, 0);
		GLES11.glEnable(GLES11.GL_LIGHT2);

		GLES11.glEnable(GLES11.GL_LIGHTING);

		GLES11.glClear(GLES11.GL_COLOR_BUFFER_BIT);
		GLES11.glHint(GLES11.GL_PERSPECTIVE_CORRECTION_HINT, GLES11.GL_NICEST);
		GLES11.glHint(GLES11.GL_POLYGON_SMOOTH_HINT, GLES11.GL_NICEST);
		GLES11.glEnable(GLES11.GL_CULL_FACE);
	}

	@Override
	public synchronized void update(float deltaTime) {
		update(deltaTime, (dialogState & DIALOG_MODAL) == 0);
	}

	private synchronized void update(float deltaTime, boolean withNavigation) {
		newScreen = null;
		for (TouchEvent event: game.getInput().getTouchEvents()) {
			if (withNavigation) {
				Screen screen = game.getNavigationBar().checkNavigationBar(event);
				if (screen != null) {
					newScreen = screen;
				}
			}
			processMessageDialogTouch(event);
			if (!isMessageDialogActive()) {
				processTouch(event);
			}
		}
		if (newScreen != null) {
			performScreenChange();
			postScreenChange();
		}
	}

	protected synchronized void updateWithoutNavigation(float deltaTime) {
		update(deltaTime, false);
	}

	boolean inFlightScreenChange() {
		if (game.getCurrentScreen() instanceof FlightScreen) {
			inFlightScreenChange(newScreen);
			return true;
		}
		return false;
	}

	public void changeFromInFlightScreen() {
		inFlightScreenChange(this);
	}

	void inFlightScreenChange(Screen screen) {
		Screen oldScreen = game.getCurrentScreen();
		if (oldScreen instanceof FlightScreen) {
			// We're in flight, so do not dispose the flight screen,
			// but the flight's information screen
			FlightScreen flightScreen = (FlightScreen) oldScreen;
			Screen oldInformationScreen = flightScreen.getInformationScreen();
			if (oldInformationScreen != null) {
				oldInformationScreen.dispose();
			}
			screen.loadAssets();
			screen.activate();
			screen.resume();
			screen.update(0);
			game.getGraphics().setClip(-1, -1, -1, -1);
			flightScreen.setInformationScreen(screen);
			game.getNavigationBar().performScreenChange();
		}
	}

	protected void performScreenChange() {
		if (inFlightScreenChange()) {
			return;
		}
		game.getCurrentScreen().dispose();
		game.setScreen(newScreen);
		game.getNavigationBar().performScreenChange();
		postScreenChange();
	}

	public void processAllTouches() {
		for (TouchEvent event : game.getInput().getTouchEvents()) {
			processTouch(event);
		}
	}

	protected void processTouch(TouchEvent touch) {
		processMessageDialogTouch(touch);
	}

	private void processMessageDialogTouch(TouchEvent touch) {
		int command = ButtonRegistry.get().processTouch(touch);
		if (touch.type != TouchEvent.TOUCH_UP) {
			return;
		}
		if (isMessageDialogActive() && (dialogState & DIALOG_MODAL) == 0) {
			if (!getDialogRect().contains(touch.x, touch.y)) {
				dialogEventsProcessed(touch, RESULT_NO);
			}
		}

		if (isMessageDialogActive() && command != Integer.MAX_VALUE) {
			dialogEventsProcessed(touch, command);
		}
	}

	private void dialogEventsProcessed(TouchEvent touch, int result) {
		if ((dialogState & DIALOG_RENDERING) != 0) {
			return;
		}
		message = null;
		ok = null;
		yes = null;
		no = null;
		SoundManager.play(Assets.click);
		if (touch != null) {
			touch.x = -1;
			touch.y = -1;
			game.getInput().getTouchEvents();
		}
		if (editText != null) {
			game.runOnUiThread(() -> {
				inputText = editText.getText().toString();
				((InputMethodManager) game.getSystemService(Context.INPUT_METHOD_SERVICE)).
					hideSoftInputFromWindow(editText.getWindowToken(), 0);
				Settings.setImmersion(((Activity)editText.getContext()).getWindow().getDecorView());
				((ViewGroup) editText.getParent()).removeView(editText);
				editText = null;
				messageResult = result;
			});
		} else {
			messageResult = result;
		}
		ButtonRegistry.get().clearMessageButtons();
		dialogState = DIALOG_OK | DIALOG_NORMAL;
	}

	final TextData[] computeCenteredTextDisplay(Graphics g, String text, int x, int y, int fieldWidth, int color) {
		return computeTextDisplay(g, text, x, y, new int[] { fieldWidth }, 45, color, Assets.regularFont, true);
	}

	protected final TextData[] computeTextDisplay(Graphics g, String text, int x, int y, int fieldWidth, int color) {
		return computeTextDisplay(g, text, x, y, new int[] { fieldWidth }, 45, color, Assets.regularFont, false);
	}

	protected final TextData[] computeTextDisplay(Graphics g, String text, int x, int y, int fieldWidth, GLText font, int color) {
		return computeTextDisplay(g, text, x, y, new int[] { fieldWidth }, 45, color, font, false);
	}

	protected final TextData[] computeTextDisplayWidths(Graphics g, String text, int x, int y, int[] fieldWidths, int color) {
		return computeTextDisplay(g, text, x, y, fieldWidths, 45, color, Assets.regularFont, false);
	}

	private TextData[] computeTextDisplay(Graphics g, String text, int x, int y, int[] fieldWidths, int deltaY, int color, GLText font, boolean centered) {
		String[] textWords = text.split(" ");
		List <TextData> resultList = new ArrayList<>();
		String result = "";
		int width = 0;
		int count = 0;
		int fieldWidth = fieldWidths[0];
		for (String word: textWords) {
			if (result.isEmpty()) {
				result = word;
				width = g.getTextWidth(result, font);
				continue;
			}
			String test = result + " " + word;
			int testWidth = g.getTextWidth(test, font);
			if (testWidth > fieldWidth) {
				int offset = centered ? fieldWidth - width >> 1 : 0;
				resultList.add(new TextData(result, x + offset, y + deltaY * count, color, font));
				count++;
				if (count < fieldWidths.length) {
					fieldWidth = fieldWidths[count];
				}
				result = word;
				width = g.getTextWidth(result, font);
			} else {
				result = test;
				width = testWidth;
			}
		}
		if (!result.isEmpty()) {
			int offset = centered ? fieldWidth - width >> 1 : 0;
			resultList.add(new TextData(result, x + offset, y + deltaY * count, color, font));
		}
		return resultList.toArray(new TextData[0]);
	}

	protected final void displayText(Graphics g, TextData[] textToDisplay) {
		if (textToDisplay == null) {
			return;
		}
		for (TextData td: textToDisplay) {
			g.drawText(td.text, td.x, td.y, td.color, td.font);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		ButtonRegistry.get().removeButtons(this);
		for (Pixmap p : pics.values()) {
			p.dispose();
		}
	}

	@Override
	public void pause() {
		if (isMessageDialogActive()) {
			dialogEventsProcessed(null, RESULT_NO);
		}
	}

	@Override
	public void resume() {
	}

	@Override
	public void postNavigationRender(float deltaTime) {
	}

	protected final void displayObject(AliteObject object, float zNear, float zFar) {
		GLES11.glEnable(GLES11.GL_TEXTURE_2D);
		GLES11.glEnable(GLES11.GL_CULL_FACE);
		GLES11.glMatrixMode(GLES11.GL_PROJECTION);
		GLES11.glLoadIdentity();
		GlUtils.gluPerspective(game, 45.0f, zNear, zFar);
		GLES11.glMatrixMode(GLES11.GL_MODELVIEW);
		GLES11.glLoadIdentity();

		GLES11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		GLES11.glEnableClientState(GLES11.GL_NORMAL_ARRAY);
		GLES11.glEnableClientState(GLES11.GL_VERTEX_ARRAY);
		GLES11.glEnableClientState(GLES11.GL_TEXTURE_COORD_ARRAY);
		GLES11.glEnable(GLES11.GL_DEPTH_TEST);
		GLES11.glDepthFunc(GLES11.GL_LESS);
		GLES11.glClear(GLES11.GL_DEPTH_BUFFER_BIT);

		GLES11.glPushMatrix();
		GLES11.glMultMatrixf(object.getMatrix(), 0);
		object.render();
		GLES11.glPopMatrix();

		GLES11.glDisable(GLES11.GL_DEPTH_TEST);
		GLES11.glDisable(GLES11.GL_TEXTURE_2D);
		setUpForDisplay();
	}

	protected final void setUpForDisplay() {
		GLES11.glDisable(GLES11.GL_CULL_FACE);
		GLES11.glDisable(GLES11.GL_LIGHTING);
		GLES11.glBindTexture(GLES11.GL_TEXTURE_2D, 0);
		GLES11.glDisable(GLES11.GL_TEXTURE_2D);

		GLES11.glMatrixMode(GLES11.GL_PROJECTION);
		GLES11.glLoadIdentity();
		GlUtils.ortho(game);

		GLES11.glMatrixMode(GLES11.GL_MODELVIEW);
		GLES11.glLoadIdentity();

		GLES11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
	}

	@Override
	public void renderNavigationBar() {
		game.getNavigationBar().render(game.getGraphics());
	}

	@Override
	public void postPresent(float deltaTime) {
		renderMessage();
	}

	@Override
	public void postScreenChange() {
	}

	public void addPictures(String... nameList) {
		for (String name : nameList) {
			pics.put(name, game.getGraphics().newPixmap(name + ".png"));
		}
	}
}
