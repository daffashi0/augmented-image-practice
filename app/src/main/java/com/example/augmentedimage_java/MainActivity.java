package com.example.augmentedimage_java;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.example.augmentedimage_java.daos.ImageDao;
import com.example.augmentedimage_java.models.Image;
import com.example.augmentedimage_java.models.ImageList;
import com.example.augmentedimage_java.models.ImageStatus;
import com.google.android.filament.Engine;
import com.google.android.filament.filamat.MaterialBuilder;
import com.google.android.filament.filamat.MaterialPackage;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.EngineInstance;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnSessionConfigurationListener {

    private Context context;

    private final List<CompletableFuture<Void>> futures = new ArrayList<>();
    private ArFragment arFragment;
    private List<Object> objectList = new ArrayList<>();
    private Object currentObjectDetected;
    private String detectedImageName;
    private String prevDetectedImageName;
    private boolean matrixDetected = false;
    private boolean rabbitDetected = false;
    private boolean womanDetected = false;
    private boolean objectRendered = false;
    private AugmentedImageDatabase database;
    private Renderable plainVideoModel;
    private Material plainVideoMaterial;
    private MediaPlayer mediaPlayer;

    private final boolean useSingleImage = false;
    private static final String TAG = MainActivity.class.getSimpleName();
    private ImageList imageList;
    private ImageDao imageDao;
    private List<ImageList> augmentedImages;
    private ImageRepository mRepository;
    private List<ImageStatus> imageStatuses = new ArrayList<ImageStatus>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String jsonFileString = Utils.getJsonFromAssets(getApplicationContext(), "image_list.json");
        Log.i("data", jsonFileString);

        Gson gson = new Gson();
        Type listUserType = new TypeToken<List<ImageList>>() { }.getType();

        augmentedImages = gson.fromJson(jsonFileString, listUserType);
        for (int i = 0; i < augmentedImages.size(); i++) {
            Log.i("images", String.valueOf(augmentedImages));
            imageStatuses.add(new ImageStatus(augmentedImages.get(i).getName(), false));
        }

//        mRepository = new ImageRepository(getApplication());
//        mRepository.getAllWords().observe(this, images -> {
//            augmentedImages = images;
//            if(images.size() != 0){
//                images.forEach(image -> imageStatuses.add(new ImageStatus(image.getName(), false)));
//            }
//        });

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            ((ViewGroup.MarginLayoutParams) toolbar.getLayoutParams()).topMargin = insets
                    .getInsets(WindowInsetsCompat.Type.systemBars())
                    .top;

            return WindowInsetsCompat.CONSUMED;
        });
        getSupportFragmentManager().addFragmentOnAttachListener(this);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }

        if(Sceneform.isSupported(this)) {
            // .glb models can be loaded at runtime when needed or when app starts
            // This method loads ModelRenderable when app starts
            loadMatrixModel();
            loadMatrixMaterial();
        }
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
        }
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        // Disable plane detection
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);

        // Images to be detected by our AR need to be added in AugmentedImageDatabase
        // This is how database is created at runtime
        // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)

        database = new AugmentedImageDatabase(session);

        // Every image has to have its own unique String identifier
        if(augmentedImages != null){
            for (int i = 0; i < augmentedImages.size(); i++){
                Bitmap object3d = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(augmentedImages.get(i).getName(), "drawable", getPackageName()));
                database.addImage(augmentedImages.get(i).getName(), object3d);
            }
        }

        config.setAugmentedImageDatabase(database);

        // Check for image detection
        arFragment.setOnAugmentedImageUpdateListener(this::onAugmentedImageTrackingUpdate);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        futures.forEach(future -> {
            if (!future.isDone())
                future.cancel(true);
        });

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
    }

    private void loadMatrixModel() {
        futures.add(ModelRenderable.builder()
                .setSource(this, Uri.parse("models/matrix.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(model -> {
                    //removing shadows for this Renderable
                    model.setShadowCaster(false);
                    model.setShadowReceiver(true);
                    plainVideoModel = model;
                })
                .exceptionally(
                        throwable -> {
                            Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG).show();
                            return null;
                        }));
    }

    private void loadMatrixMaterial() {
        Engine filamentEngine = EngineInstance.getEngine().getFilamentEngine();

        MaterialBuilder.init();
        MaterialBuilder materialBuilder = new MaterialBuilder()
                .platform(MaterialBuilder.Platform.MOBILE)
                .name("External Video Material")
                .require(MaterialBuilder.VertexAttribute.UV0)
                .shading(MaterialBuilder.Shading.UNLIT)
                .doubleSided(true)
                .samplerParameter(MaterialBuilder.SamplerType.SAMPLER_EXTERNAL, MaterialBuilder.SamplerFormat.FLOAT, MaterialBuilder.ParameterPrecision.DEFAULT, "videoTexture")
                .optimization(MaterialBuilder.Optimization.NONE);

        MaterialPackage plainVideoMaterialPackage = materialBuilder
                .blending(MaterialBuilder.BlendingMode.OPAQUE)
                .material("void material(inout MaterialInputs material) {\n" +
                        "    prepareMaterial(material);\n" +
                        "    material.baseColor = texture(materialParams_videoTexture, getUV0()).rgba;\n" +
                        "}\n")
                .build(filamentEngine);
        if (plainVideoMaterialPackage.isValid()) {
            ByteBuffer buffer = plainVideoMaterialPackage.getBuffer();
            futures.add(Material.builder()
                    .setSource(buffer)
                    .build()
                    .thenAccept(material -> {
                        plainVideoMaterial = material;
                    })
                    .exceptionally(
                            throwable -> {
                                Toast.makeText(this, "Unable to load material", Toast.LENGTH_LONG).show();
                                return null;
                            }));
        }
        MaterialBuilder.shutdown();
    }

    public void onAugmentedImageTrackingUpdate(AugmentedImage augmentedImage) {
        Log.d("LIST AR CHILDREN", arFragment.getArSceneView().getScene().getChildren().toString());
        if (augmentedImage.getTrackingState() == TrackingState.TRACKING
                && augmentedImage.getTrackingMethod() == AugmentedImage.TrackingMethod.FULL_TRACKING) {

            // Setting anchor to the center of Augmented Image
            AnchorNode anchorNode = new AnchorNode(augmentedImage.createAnchor(augmentedImage.getCenterPose()));

            // Detach 1st object if 2nd object rendered
            List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
            for (int i = 0; i < children.size(); i++) {
                Node node = children.get(i);
                if(i > 0){
                    Node nodeBefore = children.get(i-1);
                    if (node instanceof AnchorNode) {
                        if(nodeBefore instanceof AnchorNode){
                            if (((AnchorNode) nodeBefore).getAnchor() != null) {
                                ((AnchorNode) nodeBefore).getAnchor().detach();
                            }
                        }
                    }
                    if (!(nodeBefore instanceof Camera)) {
                        nodeBefore.setParent(null);
                    }
                }
            }

            // If matrix video haven't been placed yet and detected image has String identifier of "matrix"
            String imageName;
            for(int i = 0; i < augmentedImages.size(); i++){
                imageName = augmentedImages.get(i).getName();
                if(augmentedImage.getName().equals(imageName)){
                    if (findImageByName(imageStatuses, imageName).isDetected() == false){
                        if(detectedImageName != null){
                            prevDetectedImageName = detectedImageName;
                            findImageByName(imageStatuses, prevDetectedImageName).setDetected(false);
                        }
                        detectedImageName = imageName;
                        findImageByName(imageStatuses, imageName).setDetected(true);
                        if (augmentedImages.get(i).isVideo()){
                            Toast.makeText(this, imageName+" tag detected", Toast.LENGTH_LONG).show();

                            // AnchorNode placed to the detected tag and set it to the real size of the tag
                            // This will cause deformation if your AR tag has different aspect ratio than your video
                            anchorNode.setWorldScale(new Vector3(augmentedImage.getExtentX(), 1f, augmentedImage.getExtentZ()));
                            arFragment.getArSceneView().getScene().addChild(anchorNode);
                            objectRendered = true;

                            TransformableNode videoNode = new TransformableNode(arFragment.getTransformationSystem());
                            // For some reason it is shown upside down so this will rotate it correctly
                            videoNode.setLocalRotation(Quaternion.axisAngle(new Vector3(0, 1f, 0), 180f));
                            anchorNode.addChild(videoNode);

                            // Setting texture
                            ExternalTexture externalTexture = new ExternalTexture();
                            RenderableInstance renderableInstance = videoNode.setRenderable(plainVideoModel);
                            renderableInstance.setMaterial(plainVideoMaterial);

                            // Setting MediaPLayer
                            renderableInstance.getMaterial().setExternalTexture("videoTexture", externalTexture);
                            mediaPlayer = MediaPlayer.create(this, getResources().getIdentifier(imageName, "raw", getPackageName()));
                            mediaPlayer.setLooping(true);
                            mediaPlayer.setSurface(externalTexture.getSurface());
                            mediaPlayer.start();
                        } else {
                            Toast.makeText(this, imageName+" tag detected", Toast.LENGTH_LONG).show();

                            anchorNode.setWorldScale(new Vector3(0.4f, 0.4f, 0.4f));
                            arFragment.getArSceneView().getScene().addChild(anchorNode);
                            objectRendered = true;

                            futures.add(ModelRenderable.builder()
                                    .setSource(this, Uri.parse("models/"+imageName+".glb"))
                                    .setIsFilamentGltf(true)
                                    .build()
                                    .thenAccept(model3d -> {
                                        TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                                        modelNode.setRenderable(model3d);
                                        anchorNode.addChild(modelNode);
                                    })
                                    .exceptionally(
                                            throwable -> {
                                                Toast.makeText(this, "Unable to load 3d model", Toast.LENGTH_LONG).show();
                                                return null;
                                            }));
                        }
                    }
                }
            }
        }
    }

    public static ImageStatus findImageByName(List<ImageStatus> listImage, String name){
        return listImage.stream().filter(imageStatus -> name.equals(imageStatus.getName())).findFirst().orElse(null);
    }

    private void removeAnchorNode(AnchorNode nodeToremove) {
        //Remove an anchor node
        if (nodeToremove != null) {
            arFragment.getArSceneView().getScene().removeChild(nodeToremove);
            nodeToremove.getAnchor().detach();
            nodeToremove.setParent(null);
            nodeToremove = null;
            Toast.makeText(MainActivity.this, "Test Delete - anchorNode removed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Test Delete - markAnchorNode was null", Toast.LENGTH_SHORT).show();
        }
    }
}