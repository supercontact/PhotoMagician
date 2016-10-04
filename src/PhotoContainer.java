import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class PhotoContainer extends JLayeredPane implements MouseMotionListener{

	private static final long serialVersionUID = 1L;
	
	public enum ViewMode {
		PhotoViewer,
		Browser,
		Split,
		Hide
	}
	
	public final int controlPanelOpaqueHeight = 100;
	public final int controlPanelTransparentHeight = 200;
	
	public PhotoComponent mainPhoto;
	public JPanel photoIconWall;
	public ArrayList<PhotoIcon> photoIcons;
	public FadePanel controlPanel;
	public FadePanel controlPanelEditMode;
	
	private JScrollPane scrollPane;
	private IconWallMouseEventHandler iconWallHandler;
	
	private Image background;
	
	private ViewMode mode = ViewMode.Hide;
	private Point mousePos;
	private FadePanel currentControlPanel;
	private float controlPanelAlphaMultiplier = 1;
	
	public PhotoContainer() {
		initialize();
	}
	
	private void initialize() {
		scrollPane = new JScrollPane();
		add(scrollPane, 999);
		
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setOpaque(false);
		
		background = ResourceManager.backgroundImage;
		
		photoIconWall = new JPanel();
		photoIconWall.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 1));
		iconWallHandler = new IconWallMouseEventHandler(this, photoIconWall);
		photoIconWall.addMouseListener(iconWallHandler);
		photoIconWall.addMouseMotionListener(iconWallHandler);
		photoIcons = new ArrayList<>();
		for (AnnotatedPhoto photo : PhotoApplication.app.album.photoList) {
			PhotoIcon newIcon = new PhotoIcon(photo);
			photoIcons.add(newIcon);
			photoIconWall.add(newIcon);
		}
	}
	
	public void setMainPhotoComponent(PhotoComponent mainPhoto) {
		this.mainPhoto = mainPhoto;
		mainPhoto.container = this;
		mainPhoto.addMouseMotionListener(this);
	}
	
	public void switchViewMode(ViewMode newMode) {
		if (mode == newMode) return;
		
		// Clean old stuff
		if (mode == ViewMode.PhotoViewer) {
			scrollPane.setViewportView(null);
			mainPhoto.deinit();
		} else if (mode == ViewMode.Browser) {
			
		}
		
		// Set new stuff
		if (newMode == ViewMode.PhotoViewer) {
			scrollPane.setWheelScrollingEnabled(false);
			scrollPane.setViewportView(mainPhoto);
			mainPhoto.init();
		} else if (newMode == ViewMode.Browser) {
			scrollPane.setWheelScrollingEnabled(true);
			scrollPane.setViewportView(photoIconWall);
		}
		
		mode = newMode;
		updateControlPanel();
	}
	
	public void requestControlPanelHiding(float alphaMultiplier) {
		controlPanelAlphaMultiplier = alphaMultiplier;
		updateControlPanelFade();
	}
	
	@Override
	public void paintComponent(Graphics graphics) {
		paintBackground(graphics);
		
		updateScrollPane();
		updateControlPanel();
		if (mode == ViewMode.Browser) {
			updateIconWall();
		}
	}
	
	private void paintBackground(Graphics graphics) {
		// The background stays fixed when scrolling
		int w = getWidth();
		int h = getHeight();
		int imgW = background.getWidth(null);
		int imgH = background.getHeight(null);
		if (w * imgH > h * imgW) {
			// Width is too large
			int imgH2 = h * imgW / w;
			graphics.drawImage(background, 0, 0, w, h, 0, (imgH - imgH2) / 2, imgW, (imgH + imgH2) / 2, null);
		} else {
			// Height is too large
			int imgW2 = w * imgH / h;
			graphics.drawImage(background, 0, 0, w, h, (imgW - imgW2) / 2, 0, (imgW + imgW2) / 2, imgH, null);
		}
	}
	
	private void updateScrollPane() {
		scrollPane.setLocation(new Point(0, 0));
		scrollPane.setSize(getSize().width + 2, getSize().height + 1); // To avoid the 1 pixel margin at the border
		scrollPane.revalidate();
	}
	
	private void updateControlPanel() {
		if (controlPanel == null || controlPanelEditMode == null) return;
		if (mode != ViewMode.PhotoViewer) {
			controlPanel.setVisible(false);
			controlPanelEditMode.setVisible(false);
			currentControlPanel = null;
		} else if (!mainPhoto.isFlipped() && currentControlPanel != controlPanel) {
			controlPanelEditMode.setVisible(false);
			currentControlPanel = controlPanel;
		} else if (mainPhoto.isFlipped() && currentControlPanel != controlPanelEditMode) {
			controlPanel.setVisible(false);
			currentControlPanel = controlPanelEditMode;
		}
		
		if (currentControlPanel != null) {
			Dimension panelSize = currentControlPanel.getPreferredSize();
			currentControlPanel.setSize(panelSize);
			Point location = new Point();
			location.x = getWidth() / 2 - panelSize.width / 2;
			location.y = getHeight() - panelSize.height - 20;
			currentControlPanel.setLocation(location);
		}
	}

	private void updateControlPanelFade() {
		if (currentControlPanel == null) return;
		int height = getHeight() - mousePos.y;
		if (height <= controlPanelOpaqueHeight) {
			currentControlPanel.setAlpha(controlPanelAlphaMultiplier);
			currentControlPanel.setVisible(true);
		} else if (height >= controlPanelTransparentHeight) {
			currentControlPanel.setAlpha(0);
			currentControlPanel.setVisible(false);
		} else {
			currentControlPanel.setAlpha(controlPanelAlphaMultiplier * (height - controlPanelTransparentHeight) / (controlPanelOpaqueHeight - controlPanelTransparentHeight));
			currentControlPanel.setVisible(true);
		}
		currentControlPanel.repaint();
	}
	
	private void updateIconWall() {
		int epsilon = 10;
		
		Dimension wallSize = getSize();
		wallSize.width -= epsilon + scrollPane.getVerticalScrollBar().getWidth();
		photoIconWall.setPreferredSize(wallSize);
		photoIconWall.revalidate();
		
		PhotoIcon lastIcon = photoIcons.get(photoIcons.size() - 1);
		wallSize.height = lastIcon.getY() + lastIcon.getHeight();
		photoIconWall.setPreferredSize(wallSize);
		photoIconWall.revalidate();
	}

	

	@Override
	public void mouseMoved(MouseEvent e) {
		Point pos = getMousePosition(true);
		if (pos != null) {
			mousePos = pos;
			updateControlPanelFade();
		}
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		Point pos = getMousePosition(true);
		if (pos != null) {
			mousePos = pos;
			updateControlPanelFade();
		}
	}

	
	private class IconWallMouseEventHandler implements MouseListener, MouseMotionListener {
		
		private PhotoContainer container;
		private JPanel iconWall;
		private PhotoIcon currentTarget;
		
		public IconWallMouseEventHandler(PhotoContainer container, JPanel iconWall) {
			this.container = container;
			this.iconWall = iconWall;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() >= 2 && currentTarget != null) {
				container.switchViewMode(ViewMode.PhotoViewer);
				container.mainPhoto.setPhotoIndex(currentTarget.photo.getIndex());
			}
		}
		@Override
		public void mousePressed(MouseEvent e) {
			if (currentTarget != null) {
				currentTarget.setPressed(true);
				currentTarget.setSelected(!currentTarget.isSelected());
			}
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			if (currentTarget != null) {
				currentTarget.setPressed(false);
			}
		}
		@Override
		public void mouseMoved(MouseEvent e) {
			Component targetComponent = iconWall.getComponentAt(e.getPoint());
			PhotoIcon target = (targetComponent == iconWall) ? null : (PhotoIcon)targetComponent;
			if (target != currentTarget) {
				if (currentTarget != null) {
					currentTarget.setRollover(false);
				}
				if (target != null) {
					target.setRollover(true);
				}
				currentTarget = target;
			}
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			Component targetComponent = iconWall.getComponentAt(e.getPoint());
			PhotoIcon target = (targetComponent == iconWall) ? null : (PhotoIcon)targetComponent;
			if (target != currentTarget) {
				if (currentTarget != null) {
					currentTarget.setRollover(false);
					currentTarget.setPressed(false);
				}
				if (target != null) {
					target.setRollover(true);
					target.setPressed(true);
					target.setSelected(!target.isSelected());
				}
				currentTarget = target;
			}
		}
		@Override
		public void mouseEntered(MouseEvent e) {
			// Do nothing
		}
		@Override
		public void mouseExited(MouseEvent e) {
			// Do nothing
		}
	}
}
