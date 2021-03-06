package com.leobeliik.extremesoundmuffler.gui;

import com.leobeliik.extremesoundmuffler.Config;
import com.leobeliik.extremesoundmuffler.SoundMuffler;
import com.leobeliik.extremesoundmuffler.gui.buttons.MuffledSlider;
import com.leobeliik.extremesoundmuffler.gui.buttons.PlaySoundButton;
import com.leobeliik.extremesoundmuffler.utils.Anchor;
import com.leobeliik.extremesoundmuffler.utils.EventsHandler;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@OnlyIn(Dist.CLIENT)
public class SoundMufflerScreen extends Screen {

    private static final ResourceLocation GUI = new ResourceLocation(SoundMuffler.MODID, "textures/gui/sm_gui.png");
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static boolean isMuffling = true;
    private static final SortedSet<ResourceLocation> soundsList = new TreeSet<>();
    private static final SortedSet<ResourceLocation> recentSoundsList = new TreeSet<>();
    private static final Map<ResourceLocation, Double> muffledMap = new HashMap<>();
    private static final List<Anchor> anchors = new ArrayList<>();
    private static final List<Button> filteredButtons = new ArrayList<>();
    private static final Map<Button, PlaySoundButton> soundButtonList = new HashMap<>();
    private static String screenTitle = "";
    private static ITextComponent toggleSoundsListMessage;
    private final int xSize = 256;
    private final int ySize = 188;
    private final int colorWhite = 16777215;
    private final int colorViolet = 24523966;
    private final ITextComponent emptyText = StringTextComponent.EMPTY;
    private final String mainTitle = "ESM - Main Screen";
    private int minYButton;
    private int maxYButton;
    private final boolean isAnchorsDisabled = Config.getDisableAchors().get();
    private Button btnToggleMuffled;
    private Button btnDelete;
    private Button btnToggleSoundsList;
    private Button btnSetCoord;
    private Button btnEnableTitleEdit;
    private Button btnAccept;
    private Button btnCancel;
    private TextFieldWidget searchBar;
    private TextFieldWidget editTitleBar;
    private MuffledSlider slider;
    private Anchor anchor;

    private SoundMufflerScreen() {
        super(new StringTextComponent(""));
    }

    private static void open(String title, ITextComponent message) {
        toggleSoundsListMessage = message;
        screenTitle = title;
        minecraft.displayGuiScreen(new SoundMufflerScreen());
    }

    public static void open() {
        open("ESM - Main Screen", ITextComponent.getTextComponentOrEmpty("R"));
    }

    public static ResourceLocation getGUI() {
        return GUI;
    }

    public static boolean isMuffled() {
        return isMuffling;
    }

    public static void addSound(ResourceLocation sound) {
        recentSoundsList.add(sound);
    }

    public static Map<ResourceLocation, Double> getMuffledMap() {
        return muffledMap;
    }

    public static void setMuffledMap(ResourceLocation name, Double volume) {
        muffledMap.put(name, volume);
    }

    public static List<Anchor> getAnchors() {
        return anchors;
    }

    public static void setAnchor(Anchor anchor) {
        anchors.add(anchor);
    }

    @Nullable
    private static Anchor getAnchorByName(String name) {
        return anchors.stream().filter(a -> a.getName().equals(name)).findFirst().orElse(null);
    }

    @ParametersAreNonnullByDefault
    @Override
    public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrix);
        minecraft.getTextureManager().bindTexture(GUI);
        this.blit(matrix, getX(), getY(), 0, 32, xSize, ySize); //Main screen bounds
        drawCenteredString(matrix, font, screenTitle, getX() + 128, getY() + 8, colorWhite); //Screen title
        renderButtonsTextures(matrix, mouseX, mouseY);
        if (slider != null)
            slider.render(matrix, mouseX, mouseY, partialTicks);
        super.render(matrix, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        minYButton = getY() + 37;
        maxYButton = getY() + 148;

        addListener(btnToggleSoundsList = new Button(getX() + 10, getY() + 34, 10, 10, toggleSoundsListMessage, b -> {
            if (btnToggleSoundsList.getMessage().equals(ITextComponent.getTextComponentOrEmpty("R"))) {
                toggleSoundsListMessage = ITextComponent.getTextComponentOrEmpty("A");
            } else {
                toggleSoundsListMessage = ITextComponent.getTextComponentOrEmpty("R");
            }
            btnToggleSoundsList.setMessage(toggleSoundsListMessage);
            buttons.clear();
            open(screenTitle, toggleSoundsListMessage);
        }));

        addSoundButtons();

        addAnchors();

        addButton(btnToggleMuffled = new Button(getX() + 229, getY() + 165, 16, 16, emptyText, b -> isMuffling = !isMuffling)).setAlpha(0);

        addButton(btnDelete = new Button(getX() + 11, getY() + 165, 16, 16, emptyText, b -> {
                    anchor = getAnchorByName(screenTitle);
                    if (screenTitle.equals(mainTitle)) {
                        muffledMap.clear();
                        open(mainTitle, btnToggleSoundsList.getMessage());
                    } else {
                        if (anchor == null) {
                            return;
                        }
                        anchor.getMuffledSounds().clear();
                        anchor.setAnchorPos(null);
                        anchor.setName("Anchor " + anchor.getId());
                        buttons.clear();
                        open(anchor.getName(), btnToggleSoundsList.getMessage());
                    }
                })
        ).setAlpha(0);

        addButton(btnSetCoord = new Button(getX() + 260, getY() + 42, 10, 10, emptyText, b ->
                Objects.requireNonNull(getAnchorByName(screenTitle)).setAnchorPos(getPlayerPos())));
        btnSetCoord.setAlpha(0);

        addButton(editTitleBar = new TextFieldWidget(font, getX() + 258, getY() + 59, 84, 13, emptyText));
        editTitleBar.visible = false;

        addButton(btnAccept = new Button(getX() + 259, getY() + 75, 40, 20, ITextComponent.getTextComponentOrEmpty("Accept"), b -> {
            anchor = getAnchorByName(screenTitle);
            if (!editTitleBar.getText().isEmpty() && anchor != null) {
                anchor.setName(editTitleBar.getText());
                screenTitle = editTitleBar.getText();
                editTitle();
            }
        })).visible = false;

        addButton(btnCancel = new Button(getX() + 300, getY() + 75, 40, 20, ITextComponent.getTextComponentOrEmpty("Cancel"), b -> editTitle())).visible = false;

        addButton(btnEnableTitleEdit = new Button(getX() + 274, getY() + 42, 10, 10, emptyText, b -> editTitle())).setAlpha(0);

        if (screenTitle.equals(mainTitle)) {
            btnSetCoord.visible = false;
            btnEnableTitleEdit.visible = false;
        }

        addButton(searchBar = new TextFieldWidget(font, getX() + 75, getY() + 168, 105, 10, emptyText));
        searchBar.setEnableBackgroundDrawing(false);

    }

    private void addSoundButtons() {
        int buttonH = getY() + 46;
        anchor = getAnchorByName(screenTitle);

        if (!screenTitle.equals(mainTitle) && anchor == null) {
            return;
        }

        soundButtonList.clear();
        if (btnToggleSoundsList.getMessage().equals(ITextComponent.getTextComponentOrEmpty("R"))) {
            soundsList.clear();
            soundsList.addAll(recentSoundsList);
            if (screenTitle.equals(mainTitle) && !muffledMap.isEmpty()) {
                soundsList.addAll(muffledMap.keySet());
            } else if (anchor != null && !anchor.getMuffledSounds().isEmpty()) {
                soundsList.addAll(anchor.getMuffledSounds().keySet());
            }
        } else {
            soundsList.clear();
            soundsList.addAll(EventsHandler.getAllSounds());
        }

        if (soundsList.isEmpty()) {
            return;
        }

        for (ResourceLocation sound : soundsList) {
            PlaySoundButton btnPlaySound = new PlaySoundButton(getX() + 233, buttonH, new SoundEvent(sound));
            Button btnToggleSound = new Button(getX() + 221, buttonH, 10, 10, emptyText, b -> {
                double volume = 0.1F; //slider.getVolume(); //TODO make the slider prettier, with a gradient bg and things                    slider = new MuffledSlider(getX() + 14, b.y, 100, 15, ITextComponent.getTextComponentOrEmpty("Volume"), volume);
                addListener(slider = new MuffledSlider(getX() + 14, b.y, 100, 15, ITextComponent.getTextComponentOrEmpty("Volume"), volume));
                if (b.getFGColor() == colorViolet) {
                    slider.visible = true; //TODO make slider functional, probably in their own class
                    for (int i = 0; i < 100; i++) {
                    }
                    volume = slider.getVolume();
                    slider.visible = false;
                    if (screenTitle.equals(mainTitle)) {
                        muffledMap.remove(sound);
                    } else {
                        anchor.removeSound(sound);
                    }
                    b.setFGColor(colorWhite);
                    btnPlaySound.active = true;
                } else {
                    if (screenTitle.equals(mainTitle)) {
                        muffledMap.put(sound, volume);
                    } else {
                        anchor.addSound(sound, volume);
                    }
                    b.setFGColor(colorViolet);
                    btnPlaySound.active = false;
                }
            });

            boolean muffledAnchor = anchor != null && screenTitle.equals(anchor.getName()) && !anchor.getMuffledSounds().isEmpty() && anchor.getMuffledSounds().containsKey(sound);
            boolean muffledScreen = screenTitle.equals(mainTitle) && !muffledMap.isEmpty() && muffledMap.containsKey(sound);

            if (muffledAnchor || muffledScreen) {
                btnToggleSound.setFGColor(colorViolet);
                btnPlaySound.active = false;
            }

            buttonH += btnToggleSound.getHeightRealms() + 1;
            btnToggleSound.visible = btnToggleSound.y <= maxYButton;
            btnPlaySound.visible = btnPlaySound.y <= maxYButton;

            soundButtonList.put(btnToggleSound, btnPlaySound);
            addButton(btnToggleSound).setAlpha(0);
        }
        soundButtonList.forEach((v, k) -> addButton(k));
    }

    private void addAnchors() {
        int buttonW = getX() + 30;
        for (int i = 0; i <= 9; i++) {
            Button btnAnchor;
            if (isAnchorsDisabled) {
                String[] disabledMsg = {"-", "D", "i", "s", "a", "b", "l", "e", "d", "-"};
                btnAnchor = new Button(buttonW, getY() + 24, 16, 16, ITextComponent.getTextComponentOrEmpty(String.valueOf(i)), b -> {
                });
                btnAnchor.setMessage(ITextComponent.getTextComponentOrEmpty(disabledMsg[i]));
                btnAnchor.active = false;
            } else {
                int finalI = i;
                btnAnchor = new Button(buttonW, getY() + 24, 16, 16, ITextComponent.getTextComponentOrEmpty(String.valueOf(i)), b -> {
                    anchor = anchors.get(finalI);
                    if (anchor == null) return;
                    if (screenTitle.equals(anchor.getName())) {
                        screenTitle = mainTitle;
                    } else {
                        screenTitle = anchor.getName();
                    }
                    buttons.clear();
                    open(screenTitle, btnToggleSoundsList.getMessage());
                });
                int colorGreen = 3010605;
                if (!anchors.isEmpty()) {
                    btnAnchor.setFGColor(anchors.get(Integer.parseInt(btnAnchor.getMessage().getString())).getAnchorPos() != null ? colorGreen : colorWhite);
                }
            }
            addButton(btnAnchor).setAlpha(0);
            buttonW += 20;
        }
    }

    private void renderButtonsTextures(MatrixStack matrix, double mouseX, double mouseY) {
        int x; //start x point of the button
        int y; //start y point of the button
        float v; //start x point of the texture
        String message; //Button message
        int stringW; //text width
        int darkBG = -1325400064; //background color for Screen::fill()

        //Mute sound buttons and play sound buttons; Sound names

        if (buttons.size() < soundsList.size()) {
            return;
        }
        for (int i = 0; i < soundsList.size(); i++) {
            Widget btn = buttons.get(i);

            if (!btn.visible) {
                continue;
            }

            ResourceLocation rs = (ResourceLocation) soundsList.toArray()[i];
            x = getX() + 14;
            y = btn.y + 2;
            message = font.func_238412_a_(rs.getPath() + ":" + rs.getNamespace(), 200); //trim to width
            String fullMessage = rs.getPath() + ":" + rs.getNamespace();
            //draws the name of the sound; sound name : mod name
            font.drawString(matrix, message, x, y, btn.getFGColor());
            minecraft.getTextureManager().bindTexture(GUI);

            //if muffled
            v = btn.getFGColor() == colorViolet ? 10F : 0F;
            blit(matrix, getX() + 221, btn.y + 1, v, 0F, 10, 10, 80, 80); //muffle button
            blit(matrix, getX() + 233, btn.y + 1, v + 20F, 0F, 10, 10, 80, 80); //play button

            //render full names for the trim sound names when hovered
            if (font.getStringWidth(fullMessage) < 200) {
                continue;
            }
            if (mouseX > x && mouseX < x + 200 && mouseY > y && mouseY < y + font.FONT_HEIGHT) {
                fill(matrix, x - 2, y - 2, x + font.getStringWidth(fullMessage) + 2, y + font.FONT_HEIGHT + 2, -16777215);
                font.drawString(matrix, fullMessage, x, y, btn.getFGColor());
            }
        }

        //Delete button
        x = btnDelete.x + 8;
        y = btnDelete.y;
        minecraft.getTextureManager().bindTexture(GUI);

        blit(matrix, btnDelete.x, y, 0, 64F, 0F, 16, 16, 128, 128);
        message = screenTitle.equals(mainTitle) ? "Delete Muffled List" : "Delete Anchor";
        stringW = font.getStringWidth(message) / 2;
        if (btnDelete.isHovered()) {
            fill(matrix, x - stringW - 2, y + 17, x + stringW + 2, y + 31, darkBG);
            drawCenteredString(matrix, font, message, x, y + 20, colorWhite);
        }


        //toggle muffled button
        x = btnToggleMuffled.x + 8;
        y = btnToggleMuffled.y;
        if (isMuffling) {
            v = 0;
            message = "Stop Muffling";
        } else {
            v = 16F;
            message = "Start Muffling";
        }
        stringW = font.getStringWidth(message) / 2;
        minecraft.getTextureManager().bindTexture(GUI);
        blit(matrix, btnToggleMuffled.x, y, 0, v, 0F, 16, 16, 128, 128);

        if (btnToggleMuffled.isHovered()) {
            fill(matrix, x - stringW - 2, y + 18, x + stringW + 2, y + 31, darkBG);
            drawCenteredString(matrix, font, message, x, y + 20, colorWhite);
        }

        //Anchor coordinates and set coord button
        Anchor anchor = getAnchorByName(screenTitle);
        x = btnSetCoord.x;
        y = btnSetCoord.y;

        if (anchor != null) {
            int xW = font.getStringWidth(anchor.getX()) + font.getStringWidth("X: ");
            int yW = font.getStringWidth(anchor.getY()) + font.getStringWidth("Y: ");
            int zW = font.getStringWidth(anchor.getZ()) + font.getStringWidth("Z: ");
            stringW = Math.max(Math.max(xW, yW), Math.max(zW, 22));
            fill(matrix, x - 5, y - 36, x + stringW + 6, y + 16, darkBG);
            drawString(matrix, font, "X: " + anchor.getX(), x + 1, y - 30, colorWhite);
            drawString(matrix, font, "Y: " + anchor.getY(), x + 1, y - 20, colorWhite);
            drawString(matrix, font, "Z: " + anchor.getZ(), x + 1, y - 10, colorWhite);
            minecraft.getTextureManager().bindTexture(GUI);
            blit(matrix, x, y, 50, 0F, 10, 10, 80, 80); //set coordinates button
            blit(matrix, btnEnableTitleEdit.x, btnEnableTitleEdit.y, 60, 0F, 10, 10, 80, 80); //change title button
        }

        if (btnSetCoord.isHovered() && !editTitleBar.visible) {
            fill(matrix, x - 5, y + 16, x + 62, y + 40, darkBG);
            font.drawString(matrix, "Set", x, y + 20, colorWhite);
            font.drawString(matrix, "coordinates", x, y + 30, colorWhite);
        }

        message = "Edit title";
        stringW = font.getStringWidth(message) + 2;

        if (btnEnableTitleEdit.isHovered() && !editTitleBar.visible) {
            fill(matrix, x - 5, y + 16, x + stringW + 2, y + 29, darkBG);
            font.drawString(matrix, message, x, y + 18, colorWhite);
        }

        //draw anchor buttons tooltip
        for (int i = 0; i <= 9; i++) {
            Widget btn = buttons.get(soundsList.size() * 2 + i);
            x = btn.x + 8;
            y = btn.y;
            message = isAnchorsDisabled ? "Anchors are disabled" : anchors.get(i).getName();
            stringW = font.getStringWidth(message) / 2;

            if (btn.isHovered()) {
                fill(matrix, x - stringW - 2, y - 2, x + stringW + 2, y - 13, darkBG);
                drawCenteredString(matrix, font, message, x, y - 11, colorWhite);
            }
        }

        //Toggle List button draw message
        x = btnToggleSoundsList.x;
        y = btnToggleSoundsList.y;
        message = btnToggleSoundsList.getMessage().getString();
        font.drawString(matrix, message, x + 2, y + 1, 0);
        if (mouseX > x && mouseX < x + 10 && mouseY > y && mouseY < y + 10) {
            fill(matrix, x - 50, y - 22, x - 10, y + 10, darkBG);
            font.drawString(matrix, "Show", x - 48, y - 20, colorWhite);
            font.drawString(matrix, message.equals("R") ? "all" : "recent", x - 48, y - 10, colorWhite);
            font.drawString(matrix, "sounds", x - 48, y, colorWhite);
        }

        //Edit title background
        x = editTitleBar.x;
        y = editTitleBar.y;
        if (editTitleBar.visible) {
            fill(matrix, x - 2, y - 4, x + editTitleBar.getWidth() + 3, btnAccept.y + 22, darkBG);
        }
    }

    private void editTitle() {
        btnAccept.visible = !btnAccept.visible;
        btnCancel.visible = !btnCancel.visible;
        editTitleBar.setText(screenTitle);
        editTitleBar.visible = !editTitleBar.visible;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double direction) {
        if (searchBar.getText().length() > 0) {
            return filteredScroll(filteredButtons.size(), direction);
        } else {
            return normalScroll(soundsList.size(), direction);
        }
    }

    private boolean normalScroll(int size, double direction) {
        if (size <= 10) {
            return false; //enough empty screen, no need for scroll
        }
        if (buttons.get(0).y >= minYButton && direction > 0f) {
            return false; //first button is on the top
        }
        if (buttons.get(size - 1).y <= maxYButton && direction < 0f) {
            return false; //last button is on the bottom
        }

        for (int i = 0; i < size; i++) {
            Button b = (Button) buttons.get(i);
            Widget psb = soundButtonList.get(b);
            if (direction > 0) {
                b.y = b.y + (b.getHeightRealms() + 1);
                psb.y = psb.y + (b.getHeightRealms() + 1);
            } else {
                b.y = b.y - (b.getHeightRealms() + 1);
                psb.y = psb.y - (b.getHeightRealms() + 1);
            }

            if (b.y >= minYButton && b.y <= maxYButton) {
                b.visible = true;
                psb.visible = true;
            } else {
                b.visible = false;
                psb.visible = false;
            }
        }
        return true;
    }

    private boolean filteredScroll(int size, double direction) {
        if (size <= 10) {
            return false; //enough empty screen, no need for scroll
        }
        if (filteredButtons.get(0).y >= minYButton && direction > 0f) {
            return false; //first button is on the top
        }
        if (filteredButtons.get(size - 1).y <= maxYButton && direction < 0f) {
            return false; //last button is on the bottom
        }

        for (int i = 0; i < size; i++) {
            Button b = filteredButtons.get(i);
            Widget psb = soundButtonList.get(b);
            if (direction > 0) {
                b.y = b.y + (b.getHeightRealms() + 1);
                psb.y = psb.y + (b.getHeightRealms() + 1);
            } else {
                b.y = b.y - (b.getHeightRealms() + 1);
                psb.y = psb.y - (b.getHeightRealms() + 1);
            }

            if (b.y >= minYButton && b.y <= maxYButton) {
                b.visible = true;
                psb.visible = true;
            } else {
                b.visible = false;
                psb.visible = false;
            }
        }
        return true;
    }

    private void updateText() {
        int buttonH = getY() + 46;
        Object[] soundName = soundsList.toArray();
        filteredButtons.clear();
        for (int i = 0; i < soundsList.size(); i++) {
            Button b = (Button) buttons.get(i);
            Widget psb = soundButtonList.get(b);

            if (soundName[i].toString().contains(searchBar.getText())) {
                //b.active = true;
                b.y = buttonH;
                b.visible = b.y >= minYButton && b.y <= maxYButton;
                //psb.active = true;
                psb.y = buttonH;
                psb.visible = b.y >= minYButton && b.y <= maxYButton;
                filteredButtons.add(b);
                buttonH += b.getHeightRealms() + 1;
            } else {
                //b.active = true;
                b.visible = false;
                //psb.active = true;
                psb.visible = false;
            }
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        //pressed "backspace" inside search bar
        if (keyCode == 259 && searchBar.isFocused()) {
            updateText();
            return super.keyReleased(keyCode, scanCode, modifiers);
        }
        //Type inside the search bar
        if (searchBar != null && searchBar.isFocused()) {
            updateText();
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int key1, int key2, int key3) {
        //Search bar & Edit title bar looses focus when pressed "Enter" or "Intro"
        if (key1 == 257 || key1 == 335) {
            searchBar.setFocused2(false);
            editTitleBar.setFocused2(false);
            return true;
        }
        //Close screen when press "E" or the mod hotkey outside the search bar or edit title bar
        if (!searchBar.isFocused() && !editTitleBar.isFocused() && (key1 == 69 || key1 == SoundMuffler.getHotkey())) {
            onClose();
            return true;
        }
        return super.keyPressed(key1, key2, key3);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            if (searchBar.isFocused()) {
                searchBar.setText("");
                updateText();
            }
            if (editTitleBar.isFocused()) {
                editTitleBar.setText("");
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int getX() {
        return (this.width - xSize) / 2;
    }

    private int getY() {
        return (this.height - ySize) / 2;
    }

    private BlockPos getPlayerPos() {
        BlockPos player = Objects.requireNonNull(minecraft.player).getPosition();
        return new BlockPos(player.getX(), player.getY(), player.getZ());
    }

    private  <T extends Widget> void addListener(T p_addButton_1_) {
        super.children.add(p_addButton_1_);
    }
}