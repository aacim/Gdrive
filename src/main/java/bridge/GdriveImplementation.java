package bridge;

import Spec.Config;
import Spec.Specifikacija;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class GdriveImplementation implements Specifikacija {

    public Drive service;
    public Config konf=null;
    public String driveRootID = "1HZIVq5tVoCjivaHqgDpo0TslgcOIwYae";

    private void restoreDriveRootID(){
        driveRootID = "1HZIVq5tVoCjivaHqgDpo0TslgcOIwYae";
    }

    private String findInFolder(String parentID, String childName){

        String childID=null;

        String query = null;


        query = " '" + parentID + "' in parents and name contains '" + childName + "' ";

        FileList result = null;
        try {
            result = service.files().list().setQ(query)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();

        if (files.isEmpty()) return null;

        childID=files.get(0).getId();

        return childID;
    }

    private String getTargetFromPath(String path){
        String fileID=null;
        String[] folders= path.split("/");

        System.out.println("Gettarget krece od " + driveRootID);
        String targetID=driveRootID;
        for (String a : folders) {
            targetID=findInFolder(targetID,a);
            System.out.println("U folderu: " + a + " ID: " + targetID + " \n" );
        }

        fileID=targetID;


        return fileID;
    }


    public GdriveImplementation() {
        try {
            quickstart.DriveQuickstart.ucitaj();
            service = quickstart.servis;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private int loadConfig(String storageLoc){


    /*
        konf = new Config(new java.io.File("radni/config.json"));

        konf.setOccupied(true);

        System.out.println(konf.getJSONForm());

    */

        return 0;
    }




    @Override
    public int connectStorage(String path) {

        String storageLocID = getTargetFromPath(path);

        String found = findInFolder(storageLocID,"config.json");

        if (found==null){
            System.out.println("Nije pronadjen config.json u " + path);

        } else {
            System.out.println("Pronadjen config: " + found);
            driveRootID=storageLocID;
            System.out.println("Novi root: " + driveRootID);

            loadConfig(path);



        }




        return 0;
    }




    @Override
    public int requestNewUser() {
        return 0;
    }


    @Override
    public int disconnectStorage() {
        restoreDriveRootID();
        konf.setOccupied(false);
        return 0;
    }


    @Override
    public int createFile(String newFileName, String destin) {
        String folderID = getTargetFromPath(destin);
        File fileMetadata = new File();
        fileMetadata.setName(newFileName);
        fileMetadata.setParents(Collections.singletonList(folderID));

        File file = null;
        try {
            file = service.files().create(fileMetadata)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Folder name: "+ file.getName() + "Folder ID: " + file.getId());

        return 0;
    }

    @Override
    public int createFolder(String newFolderName, String destin) {

        String folderID = getTargetFromPath(destin);
        File fileMetadata = new File();
        fileMetadata.setName(newFolderName);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        fileMetadata.setParents(Collections.singletonList(folderID));

        File file = null;
        try {
            file = service.files().create(fileMetadata)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Created file name: "+ file.getName() + " file ID: " + file.getId());

        return 0;
    }

    @Override
    public int uploadFile(String target, String destin) {

        String folderID = getTargetFromPath(destin);


        File fileMetadata = new File();
        fileMetadata.setName(target);
        fileMetadata.setMimeType("application/vnd.google-apps.file");
        fileMetadata.setParents(Collections.singletonList(folderID));

        java.io.File filePath = new java.io.File(target);
        FileContent mediaContent = new FileContent("text/pdf", filePath);
        File file = null;
        try {
            file = service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("File ID: " + file.getId());

        return 0;
    }

    @Override
    public int uploadFiles(List<String> list, List<String> list1) {
        return 0;
    }

    @Override
    public int deleteFile(String s) {
        //TODO
        deleteFolder(s);
        return 0;
    }


    @Override
    public int deleteFolder(String s) {

        String deleteID = getTargetFromPath(s);

        try {
            System.out.println("Brisem: " + deleteID);
            service.files().delete(deleteID).execute();
        } catch (IOException e) {
            System.out.println("Greska pri brisanju: " + e);
            return 1;
        }

        return 0;
    }

    @Override
    public List<String> listFiles(String targetPath) {

        String googleFolderIdParent;

        if (targetPath.trim()=="")
            googleFolderIdParent=driveRootID;
        else
            googleFolderIdParent = getTargetFromPath(targetPath);


        String query = null;

        query = " '" + googleFolderIdParent + "' in parents";


        FileList result = null;
        try {
            result = service.files().list().setQ(query)
                    .setPageSize(50)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files in " + googleFolderIdParent + ":");
            for (File file : files) {
                System.out.printf("%s, %s \n", file.getName(), file.getId());
            }
        }

        return null;
    }

    @Override
    public int moveFile(String toMove, String dest) {
        String fileId = getTargetFromPath(toMove);
        String folderId = getTargetFromPath(dest);

        File file = null;
        try {
            file = service.files().get(fileId)
                    .setFields("parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder previousParents = new StringBuilder();
        for (String parent : file.getParents()) {
            previousParents.append(parent);
            previousParents.append(',');
        }

        try {
            file = service.files().update(fileId, null)
                    .setAddParents(folderId)
                    .setRemoveParents(previousParents.toString())
                    .setFields("id, parents")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }


    @Override
    public int downloadFolder(String s, String s1) {
        return 5;
    }


    @Override
    public int downloadFile(String toDownload, String destin) {
        String fileId = getTargetFromPath(toDownload);

        System.out.println(":\n" + destin + "\n");
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(destin.trim());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            service.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }



        return 0;
    }

    @Override
    public int addExtBan(String s) {
        return 0;
    }

    @Override
    public int removeExtBan(String s) {
        return 0;
    }

}