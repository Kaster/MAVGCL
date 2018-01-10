package com.comino.flight.ui.widgets.view3D.objects;

import com.comino.flight.observables.StateProperties;
import com.comino.flight.ui.widgets.view3D.utils.Xform;
import com.comino.msp.model.DataModel;
import com.comino.msp.utils.MSPMathUtils;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;

public class Camera extends Xform {

	private static final double CAMERA_INITIAL_DISTANCE 	= -1500;
	private static final double CAMERA_INITIAL_X_ANGLE  	= -10.0;
	private static final double CAMERA_INITIAL_Y_ANGLE  	=  0.0;
	private static final double CAMERA_NEAR_CLIP 		= 	0.1;
	private static final double CAMERA_FAR_CLIP 			= 100000.0;

	private final PerspectiveCamera camera = new PerspectiveCamera(true);
	private final Xform cameraXform2 = new Xform();
	private final Xform cameraXform3 = new Xform();

	private static final double CONTROL_MULTIPLIER = 0.1;
	private static final double SHIFT_MULTIPLIER = 10.0;
	private static final double MOUSE_SPEED = 0.1;
	private static final double ROTATION_SPEED = 2.0;

	private double mousePosX;
	private double mousePosY;
	private double mouseOldX;
	private double mouseOldY;
	private double mouseDeltaX;
	private double mouseDeltaY;

	public Camera(final SubScene scene) {

		super();

		this.getChildren().add(cameraXform2);
		cameraXform2.getChildren().add(cameraXform3);
		cameraXform3.getChildren().add(camera);

		this.ry.setAngle(CAMERA_INITIAL_Y_ANGLE);
		this.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
		this.setRotateZ(180.0);

		camera.setNearClip(CAMERA_NEAR_CLIP);
		camera.setFarClip(CAMERA_FAR_CLIP);
		camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);

		scene.setCamera(camera);

		camera.setVisible(true);

		registerHandlers(scene);

	}

	public void updateState(DataModel model) {
		camera.setTranslateX(-model.state.l_y*100);
		camera.setTranslateY(model.state.l_z > 0 ? 0 : model.state.l_z *100);
		camera.setTranslateZ( model.state.l_x*100);
		this.setRy(MSPMathUtils.fromRad(model.attitude.y));
	}

	private void registerHandlers(final Node node) {

		node.setOnScroll(event -> {
			camera.setTranslateX(camera.getTranslateX() + event.getDeltaX()*MOUSE_SPEED);
			camera.setTranslateY(camera.getTranslateY() + event.getDeltaY()*MOUSE_SPEED);
		});

		node.setOnMousePressed((me) -> {
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseOldX = me.getSceneX();
			mouseOldY = me.getSceneY();
		});

		node.setOnMouseDragged((me) -> {
			mouseOldX = mousePosX;
			mouseOldY = mousePosY;
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseDeltaX = -(mousePosX - mouseOldX);
			mouseDeltaY = -(mousePosY - mouseOldY);

			double modifier = 1.0;

			if (me.isControlDown()) {
				modifier = CONTROL_MULTIPLIER;
			}
			if (me.isShiftDown()) {
				modifier = SHIFT_MULTIPLIER;
			}

			if (me.isPrimaryButtonDown()) {
				this.ry.setAngle(this.ry.getAngle() -
						mouseDeltaX*MOUSE_SPEED*modifier*ROTATION_SPEED);  //
						this.rx.setAngle(this.rx.getAngle() +
								mouseDeltaY*MOUSE_SPEED*modifier*ROTATION_SPEED);  // -
			}
		});

		node.setOnZoom(event -> {
			camera.setTranslateZ(camera.getTranslateZ() + (event.getZoomFactor()-1)*MOUSE_SPEED*3000);
		});

	}

}