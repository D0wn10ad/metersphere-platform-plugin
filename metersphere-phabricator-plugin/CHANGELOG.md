# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-02-10

### Added
- Initial release of MeterSphere Phabricator plugin
- Phabricator Conduit API integration with authentication via API tokens
- Support for 4 project mapping strategies:
  - ROOT_ONLY: Always map to root project
  - CURRENT_CONTEXT: Use current project context  
  - HIERARCHICAL: Maintain full hierarchy path
  - SMART_RESOLUTION: Automatic context detection
- Mandatory project tags with complete hierarchy visibility
- Default project fallback when primary mapping fails
- Configurable closure status handling (Phabricator doesn't support deletion)
  - Default statuses: resolved, wontfix, invalid, spite, duplicate
  - Customizable per installation
- Maniphest subtype support (Task, Story, Epic)
- Remarkup to Markdown conversion using remarkup.process API
- Link-based attachment handling (proxy links instead of file sync)
- Complete frontend configuration (frontend.json) for MeterSphere UI
- Comprehensive implementation documentation:
  - README.md with setup instructions
  - CHANGELOG.md (this file)
  - CONFIGURATION.md with detailed options

### Technical Details
- Java 21 compatibility
- Maven build system
- Compatible with MeterSphere v2.10.x
- Uses Phabricator Conduit API stable endpoints
- AI-generated codebase (OpenCode AI)

### Notes
- This is an AI-generated prototype requiring review before production use
- Platform interface implementation uses stub classes for compilation
- Some @Override annotations are commented out for stub compatibility
- Requires alignment with actual MeterSphere SDK for production deployment

### Known Limitations
- Stub domain classes used for compilation (IssuesWithBLOBs, etc.)
- Platform interface methods need review against actual MeterSphere SDK
- Integration testing with real Phabricator instance pending

---

## Future Enhancements (Planned)

### v0.2.0 (TBD)
- [ ] Complete Platform interface alignment
- [ ] Integration tests with real Phabricator instance
- [ ] Performance optimization for large datasets
- [ ] Attachment synchronization improvements
- [ ] Custom field mapping support

### v1.0.0 (TBD)
- [ ] Production-ready release
- [ ] Full MeterSphere v2.10 compatibility
- [ ] Comprehensive test coverage
- [ ] Documentation improvements
- [ ] Migration guide from other platforms

---

## Security Notes

- API tokens should be stored securely in MeterSphere
- SSL/TLS verification enabled by default
- No sensitive data logged
- Proxy support available for corporate environments

## AI Attribution

This plugin was generated with AI assistance using OpenCode AI. The codebase serves as a functional prototype and requires:
- Code review
- Security audit  
- Integration testing
- Performance validation

before production deployment.