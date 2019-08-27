package com.twb.bitbucketbackup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SpringBootApplication
public class BitbucketBackupApplication implements CommandLineRunner {

    private static final String CLONE_DIR = "paclones";

    private static final String USERNAME = "thomasbigger584";
    private static final String PASSWORD = "basketball1";

    private static Logger log = LoggerFactory.getLogger(BitbucketBackupApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BitbucketBackupApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String userHomePath = System.getProperty("user.home");

        File rootCloneDir = new File(userHomePath + File.separator + CLONE_DIR);
        if (!rootCloneDir.exists()) {
            rootCloneDir.mkdirs();
        }

        List<RepositoryToClone> repositoryToCloneList = new ArrayList<>();

        AuthorizedRestTemplate restTemplate = new AuthorizedRestTemplate(USERNAME, PASSWORD);

        for (int page = 1; page < 30; page++) {
            final String uriString = "https://api.bitbucket.org/2.0/repositories/performanceactive?page=" + page;
            String response = restTemplate.getForObject(uriString);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode responseJson = mapper.readTree(response);

            JsonNode valuesJsonArray = responseJson.get("values");
            if (valuesJsonArray.isArray()) {

                for (Iterator<JsonNode> jsonIterator = valuesJsonArray.elements(); jsonIterator.hasNext(); ) {
                    JsonNode valuesNode = jsonIterator.next();

                    if (valuesNode.has("links")) {
                        JsonNode linksNode = valuesNode.get("links");
                        if (linksNode.has("clone")) {

                            JsonNode cloneNodeArray = linksNode.get("clone");

                            if (cloneNodeArray.isArray()) {
                                for (Iterator<JsonNode> cloneIterator = cloneNodeArray.elements(); cloneIterator.hasNext(); ) {
                                    JsonNode cloneNode = cloneIterator.next();
                                    if (cloneNode.has("name")) {
                                        String cloneUrl = cloneNode.get("name").asText();
                                        if (cloneUrl.startsWith("http")) {

                                            RepositoryToClone repositoryToClone = new RepositoryToClone();
                                            String repoName = valuesNode.get("full_name").asText();
                                            repositoryToClone.setName(repoName);
                                            log.info(repoName);
                                            repositoryToClone.setCloneUrl(cloneNode.get("href").asText());
                                            repositoryToClone.setLastUpdatedDate(valuesNode.get("updated_on").asText());
                                            repositoryToCloneList.add(repositoryToClone);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        for (RepositoryToClone repositoryToClone : repositoryToCloneList) {
            File repoDir = new File(rootCloneDir, repositoryToClone.getName());
            try {
                log.info("Cloning " + repositoryToClone.getName() + " into " + repositoryToClone.getCloneUrl());
                Git.cloneRepository()
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(USERNAME, PASSWORD))
                        .setURI(repositoryToClone.getCloneUrl())
                        .setDirectory(repoDir)
                        .call();
            } catch (GitAPIException e) {
                log.error("Exception occurred while cloning " + repositoryToClone.getName());
                e.printStackTrace();
            }
        }
    }
}
