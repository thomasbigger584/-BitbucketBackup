package com.twb.bitbucketbackup;

import lombok.Data;

@Data
public class RepositoryToClone {
    private String name;
    private String cloneUrl;
    private String lastUpdatedDate;
}
