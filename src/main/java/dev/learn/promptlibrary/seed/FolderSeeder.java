package dev.learn.promptlibrary.seed;

import dev.learn.promptlibrary.folder.Folder;
import dev.learn.promptlibrary.folder.FolderRepository;
import dev.learn.promptlibrary.folder.FolderService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Gives each of the 50 demo users a root default folder and one nested
 * demonstration folder. The default get-or-create call keeps reruns safe.
 */
@ApplicationScoped
public class FolderSeeder implements Seeder {

    private final FolderRepository folders;
    private final FolderService folderService;

    public FolderSeeder(FolderRepository folders, FolderService folderService) {
        this.folders = folders;
        this.folderService = folderService;
    }

    @Override
    public int order() {
        return 15;
    }

    @Override
    @Transactional
    public void seed() {
        for (String owner : SeedUsers.all()) {
            Folder defaultFolder = folderService.ensureDefaultFolder(owner);
            String name = "folder-01";
            if (folders.findByOwnerAndName(owner, name).isPresent()) {
                continue;
            }

            Folder folder = new Folder();
            folder.setOwnerId(owner);
            folder.setName(name);
            folder.setParent(defaultFolder);
            folders.persist(folder);
        }
    }
}
